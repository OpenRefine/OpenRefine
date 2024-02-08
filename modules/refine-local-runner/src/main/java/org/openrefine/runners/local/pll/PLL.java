
package org.openrefine.runners.local.pll;

import java.io.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.vavr.collection.Array;
import io.vavr.collection.Iterator;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
import org.apache.commons.lang.Validate;

import org.openrefine.model.Grid;
import org.openrefine.model.Runner;
import org.openrefine.process.ProgressingFuture;
import org.openrefine.process.ProgressingFutures;
import org.openrefine.runners.local.pll.util.IterationContext;
import org.openrefine.runners.local.pll.util.ProgressingFutureWrapper;
import org.openrefine.runners.local.pll.util.QueryTree;
import org.openrefine.runners.local.pll.util.TaskSignalling;
import org.openrefine.util.CloseableIterator;

/**
 * A Partitioned Lazy List (PLL) is a lazily-computed immutable container data structure to represent lists of elements.
 * <p>
 * It is split into contiguous partitions, enabling efficient parallel processing. It is analogous to Spark's Resilient
 * Distributed Datasets (RDD) in spirit, but it is not designed for distributed contexts: a PLL is local to a given JVM.
 * This removes the need for any serialization of jobs or of shuffled data. The API offered by PLL is also more modest,
 * since its only purpose is to fulfill the requirements of the {@link Grid} interface.
 * <p>
 * Running Spark in standalone mode is only designed for local testing and does not remove the overhead of serialization
 * and scheduling.
 * 
 * @author Antonin Delpeuch
 *
 */
public abstract class PLL<T> {

    protected final PLLContext context;

    // id of the PLL allocated by the context
    protected final long id;
    // short description of the operation this PLL implements, for debugging purposes
    protected final String name;

    // cached list of counts of elements in each partition, initialized lazily
    private Array<Long> cachedPartitionSizes;
    // cached contents of each partition, initialized on demand
    protected Array<Array<T>> cachedPartitions;

    public PLL(PLLContext context, String name) {
        Validate.notNull(context);
        this.context = context;
        this.cachedPartitionSizes = null;
        this.cachedPartitions = null;
        this.id = context.allocateId();
        this.name = name;
    }

    /**
     * Iterate over the elements of the given partition. This is the method that should be implemented by subclasses. As
     * this method forces computation, ignoring any caching, consumers should not call it directly but rather use
     * {@link #iterate(Partition)}. Once the iterator is not needed anymore, it should be closed. This makes it possible
     * to release the underlying resources supporting it, such as open files or sockets.
     * 
     * @param partition
     *            the partition to iterate over
     * @param context
     * @return
     */
    protected abstract CloseableIterator<T> compute(Partition partition, IterationContext context);

    /**
     * @return the number of partitions in this list
     */
    public int numPartitions() {
        return getPartitions().size();
    }

    /**
     * @return the partitions in this list
     */
    public abstract Array<? extends Partition> getPartitions();

    /**
     * Iterate over the elements of the given partition. If the contents of this PLL have been cached, this will iterate
     * from the cache instead. Iteration is done with the default {@link IterationContext}.
     *
     * @param partition
     *            the partition to iterate over
     */
    public CloseableIterator<T> iterate(Partition partition) {
        return iterate(partition, IterationContext.DEFAULT);
    }

    /**
     * Iterate over the elements of the given partition. If the contents of this PLL have been cached, this will iterate
     * from the cache instead.
     *
     * @param partition
     *            the partition to iterate over
     * @param context
     *            some additional information to configure the iteration, relaxing the completeness requirements of the
     *            generated data
     */
    public CloseableIterator<T> iterate(Partition partition, IterationContext context) {
        if (cachedPartitions != null) {
            return CloseableIterator.wrapping(cachedPartitions.get(partition.getIndex()).iterator());
        } else {
            return compute(partition, context);
        }
    }

    /**
     * @return the total number of elements
     */
    public long count() {
        return getPartitionSizes()
                .sum().longValue();
    }

    /**
     * Returns the number of elements in each partition. See {@link #hasCachedPartitionSizes()} to check if those sizes
     * are already computed. Subclasses should rather override {@link #computePartitionSizes()} if they can do this
     * computation more efficiently than by iterating over the partitions.
     */
    public final Array<Long> getPartitionSizes() {
        if (cachedPartitionSizes == null) {
            cachedPartitionSizes = computePartitionSizes();
        }
        return cachedPartitionSizes;
    }

    protected Array<Long> computePartitionSizes() {
        return runOnPartitionsWithoutInterruption(p -> {
            try (CloseableIterator<T> iterator = iterate(p)) {
                return (long) iterator.size();
            }
        });
    }

    /**
     * @return the list of all elements in the list, retrieved in memory.
     */
    public Array<T> collect() {
        Array<Array<T>> partitionLists = null;
        try {
            partitionLists = collectPartitionsAsync().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new PLLExecutionError(e);
        }

        return partitionLists.flatMap(t -> t);
    }

    /**
     * Retrieves the contents of all partitions. This does not store them in the local cache, so two successive calls to
     * this method will enumerate the contents of the PLL twice.
     */
    protected ProgressingFuture<Array<Array<T>>> collectPartitionsAsync() {
        Array<Array<T>> results;
        if (cachedPartitions != null) {
            return ProgressingFutures.immediate(cachedPartitions);
        } else {
            ProgressingFuture<Array<Array<T>>> future = runOnPartitionsAsync((partition, signalling) -> {
                try (CloseableIterator<T> iterator = signalling.wrapStream(iterate(partition), 100, partition.getIndex() * 5)) {
                    return Array.ofAll(iterator);
                }
            }, 0);

            return ProgressingFutures.transform(future,
                    partitions -> {
                        if (cachedPartitionSizes == null) {
                            cachedPartitionSizes = partitions
                                    .map(l -> (long) l.size());
                        }
                        return partitions;
                    }, context.getExecutorService());
        }
    }

    /**
     * Returns an iterator over the list. This may contain incomplete elements if the underlying collection is being
     * computed.
     *
     * @see #blockingIterator()
     */
    public CloseableIterator<T> iterator() {
        return iterateFromPartition(0);
    }

    /**
     * Returns an iterator over the list, blocking synchronously if we are encountering elements that are not fuly
     * computed. They will only be enumerated once they are fully computed.
     *
     * @see #iterator()
     */
    public CloseableIterator<T> blockingIterator() {
        return iterateFromPartition(0, IterationContext.SYNCHRONOUS_DEFAULT);
    }

    /**
     * Returns an iterator over the list, with the settings of the iteration described by the parameter supplied.
     *
     * @see #iterator()
     * @see #blockingIterator()
     */
    public CloseableIterator<T> iterator(IterationContext context) {
        return iterateFromPartition(0, context);
    }

    /**
     * Stream over the part of the collection that starts at given partition boundary. The iteration is done with the
     * default iteration context, {@link IterationContext#DEFAULT}.
     *
     * @param partitionId
     *            the index of the partition to start enumerating from
     * @see #iterateFromPartition(int, IterationContext)
     */
    protected CloseableIterator<T> iterateFromPartition(int partitionId) {
        return iterateFromPartition(partitionId, IterationContext.DEFAULT);
    }

    /**
     * Stream over the part of the collection that starts at given partition boundary.
     *
     * @param partitionId
     *            the index of the partition to start enumerating from
     * @param context
     *            the iteration context to use
     */
    protected CloseableIterator<T> iterateFromPartition(int partitionId, IterationContext context) {
        CloseableIterator<? extends Partition> partitions = CloseableIterator.wrapping(getPartitions().iterator().drop(partitionId));
        if (cachedPartitions != null) {
            return CloseableIterator.wrapping(partitions
                    .flatMap(p -> cachedPartitions.get(p.getIndex())));
        } else {
            return partitions
                    .flatMapCloseable(partition -> iterate(partition, context));
        }
    }

    /**
     * Is this list empty?
     */
    public boolean isEmpty() {
        if (cachedPartitionSizes != null) {
            // by doing this we avoid any enumeration from the partitions
            return count() == 0;
        } else {
            try (CloseableIterator<T> iterator = iterator()) {
                return !iterator.hasNext();
            }
        }
    }

    /**
     * Returns the n first elements of the list (or less if there are less elements in the list).
     */
    public io.vavr.collection.List<T> take(int num) {
        if (num < 0) {
            throw new IllegalArgumentException("The number of elements to take must be non-negative");
        }
        try (CloseableIterator<T> iterator = iterator()) {
            return iterator.take(num).toList();
        }
    }

    /**
     * Runs an associative aggregation function on the list.
     * 
     * @param <U>
     * @param initialValue
     *            the neutral value for the combine operation
     * @param map
     *            a function taking the current state, a list element and returning the updated aggregation
     * @param combine
     *            the associative operator
     * @return the aggregated value over the entire list
     */
    public <U> U aggregate(U initialValue, BiFunction<U, T, U> map, BiFunction<U, U, U> combine) {
        Array<U> states = runOnPartitionsWithoutInterruption(partition -> {
            try (CloseableIterator<T> iterator = iterate(partition)) {
                return iterator.foldLeft(initialValue, map);
            }
        });
        return states.fold(initialValue, combine);
    }

    // PLL derivations

    /**
     * Derives a new PLL by applying a map function on each element of this list. The function is applied lazily, so it
     * can be called multiple times on the same element, depending on the actions called on the returned PLL.
     * 
     * @param <U>
     * @param mapFunction
     *            the function to apply on each element
     * @param mapDescription
     *            a short descriptiono of the function, for debugging purposes
     * @return
     */
    public <U> PLL<U> map(Function<T, U> mapFunction, String mapDescription) {
        BiFunction<Integer, CloseableIterator<T>, CloseableIterator<U>> partitionMap = ((i, parentIterator) -> parentIterator
                .map(mapFunction));
        return mapPartitions(partitionMap, "Map: " + mapDescription, true);
    }

    /**
     * Derives a new PLL by applying a map function on each element, which can return multiple elements of the new list.
     * The function is applied lazily, so it can be called multiple times on the same element, depending on the actions
     * called on the returned PLL.
     * 
     * @param <U>
     * @param mapFunction
     * @return
     */
    public <U> PLL<U> flatMap(Function<T, CloseableIterator<U>> mapFunction, String mapDescription) {
        BiFunction<Integer, CloseableIterator<T>, CloseableIterator<U>> partitionMap = ((i, parentStream) -> parentStream
                .flatMapCloseable(mapFunction));
        return mapPartitions(partitionMap, "FlatMap: " + mapDescription, false);
    }

    /**
     * Groups elements by batches of the desired size, in each partition. At the end of partitions, groups of smaller
     * size may be created even if there are more elements in the following partitions.
     * 
     * @param batchSize
     *            the desired maximal size of batches
     * @return
     */
    public PLL<List<T>> batchPartitions(int batchSize) {
        return mapPartitions((i, parentStream) -> parentStream.grouped(batchSize).map(seq -> seq.toJavaList()),
                String.format("Group into batches of %d elements", batchSize),
                batchSize == 1);
    }

    /**
     * Maps each partition by applying an arbitrary function to it.
     * 
     * @param <U>
     * @param map
     *            the function to apply on the stream of elements of the partition
     * @param mapDescription
     *            a short description of the map function, for debugging purposes
     * @param preservesSizes
     *            whether the size of each partition will be preserved by the map
     * @return
     */
    public <U> PLL<U> mapPartitions(
            BiFunction<Integer, CloseableIterator<T>, CloseableIterator<U>> map,
            String mapDescription,
            boolean preservesSizes) {
        return new MapPartitionsPLL<T, U>(this, map, mapDescription, preservesSizes);
    }

    /**
     * Applies a map function on the list, such that the map function is able to keep a state from one element to the
     * other. This state is required to be combinable with an associative and unital function.
     * 
     * @param <S>
     *            the type of the state kept by the mapper
     * @param <U>
     *            the type of elements returned by the mapper
     * @param initialState
     *            the initial state
     * @param feed
     *            the function to update the state after each element
     * @param combine
     *            the function to combine two states, at partition boundaries
     * @param map
     *            the mapper itself
     * @return
     */
    public <S, U> PLL<U> scanMap(S initialState, Function<T, S> feed, BiFunction<S, S, S> combine, BiFunction<S, T, U> map) {
        // Compute the initial states at the beginning of each partition
        Array<? extends Partition> partitions = getPartitions();
        Function<Partition, S> partitionFunction = partition -> {
            try (CloseableIterator<T> iterator = iterate(partition)) {
                return iterator.foldLeft(initialState,
                        (state, element) -> combine.apply(state, feed.apply(element)));
            }
        };
        Array<S> partitionStates = runOnPartitionsWithoutInterruption(partitionFunction,
                partitions.take(partitions.size() - 1).iterator());
        List<S> initialStates = new ArrayList<>(numPartitions());
        S currentState = initialState;
        for (int i = 0; i != numPartitions(); i++) {
            initialStates.add(currentState);
            if (i < partitionStates.size()) {
                currentState = combine.apply(currentState, partitionStates.get(i));
            }
        }

        BiFunction<Integer, CloseableIterator<T>, CloseableIterator<U>> partitionMap = ((i, stream) -> scanMapStream(stream,
                initialStates.get(i), feed, combine,
                map));
        return mapPartitions(partitionMap, "scan map", true);
    }

    protected static <T, S, U> CloseableIterator<U> scanMapStream(
            CloseableIterator<T> iterator,
            S initialState,
            Function<T, S> feed,
            BiFunction<S, S, S> combine,
            BiFunction<S, T, U> map) {
        return new CloseableIterator<U>() {

            private S currentState = initialState;

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public U next() {
                T element = iterator.next();
                U result = map.apply(currentState, element);
                currentState = combine.apply(currentState, feed.apply(element));
                return result;
            }

            @Override
            public void close() {
                iterator.close();
            }

        };
    }

    /**
     * Derives a new PLL by filtering the collection to only contain elements which match the supplied predicate. The
     * predicate is evaluated lazily, so it can be called multiple times on the same element, depending on the actions
     * called on the returned PLL.
     * 
     * @param filterPredicate
     * @return
     */
    public PLL<T> filter(Predicate<? super T> filterPredicate) {
        return mapPartitions((i, parentIterator) -> parentIterator.filter(filterPredicate), "filter", false);
    }

    /**
     * Maps this collection to an indexed PLL. This does not come with any partitioner, so it is only indexed in the
     * sense that it offers specific methods for collections of pairs.
     * 
     * @param <K>
     * @param <V>
     * @param mapFunction
     * @return
     * @deprecated use {@link #mapToPair(Function, String)} to also provide a description of the map function applied
     */
    @Deprecated
    public <K, V> PairPLL<K, V> mapToPair(Function<T, Tuple2<K, V>> mapFunction) {
        return new PairPLL<K, V>(this.map(mapFunction, "unknown map function"), Optional.empty());
    }

    /**
     * Maps this collection to an indexed PLL. This does not come with any partitioner, so it is only indexed in the
     * sense that it offers specific methods for collections of pairs.
     *
     * @param <K>
     * @param <V>
     * @param mapFunction
     *            the function to apply on each element
     * @param mapDescription
     *            a short description of the map function being applied, for debugging purposes
     * @return
     */
    public <K, V> PairPLL<K, V> mapToPair(Function<T, Tuple2<K, V>> mapFunction, String mapDescription) {
        return new PairPLL<K, V>(this.map(mapFunction, mapDescription), Optional.empty());
    }

    /**
     * Indexes the collection in sequential order. This creates a partitioner, making it efficient to retrieve an
     * element by index with {@link PairPLL#get}.
     */
    public PairPLL<Long, T> zipWithIndex() {
        return IndexedPLL.index(this);
    }

    /**
     * Sorts the collection using the supplied comparator. This fetches the entire collection in memory.
     * 
     * @param comparator
     * @return
     */
    public PLL<T> sort(Comparator<T> comparator) {
        List<T> sorted = collect().toJavaList();
        sorted.sort(comparator);
        return new InMemoryPLL<T>(context, sorted, numPartitions());
    }

    /**
     * Concatenates another PLL at the end of this one, resulting in a new PLL. The new PLL has the union of the
     * partitions of both original PLLs as partition set.
     * 
     * @param other
     *            the list of elements to add at the end of this one
     */
    public PLL<T> concatenate(PLL<T> other) {
        return new UnionPLL<T>(Arrays.asList(this, other));
    }

    /**
     * Concatenates other PLLs at the end of this one, resulting in a new PLL. The new PLL has the union of the
     * partitions of all original PLLs as partition set.
     *
     * @param others
     *            the list of PLLs to add at the end of this one
     */
    public PLL<T> concatenate(List<PLL<T>> others) {
        List<PLL<T>> fullList = new ArrayList<>(others.size() + 1);
        fullList.add(this);
        fullList.addAll(others);
        return new UnionPLL<T>(fullList);
    }

    /**
     * Limit each partition to contain only their first N elements.
     * 
     * @param limit
     *            the maximum number of items per partition
     * @return
     */
    public PLL<T> limitPartitions(long limit) {
        if (limit == 0L) {
            // We preserve the number of partitions
            return new InMemoryPLL<T>(this.context, Collections.emptyList(), numPartitions());
        } else {
            return new MapPartitionsPLL<T, T>(this, (i, iterator) -> iterator.take((int) limit),
                    String.format("Limit each partition to %d", limit));
        }
    }

    /**
     * Only retain partitions designated by the given list of indices. The other partitions are dropped from the PLL.
     * The partitions in the resulting PLL are ordered according to the list of indices supplied.
     *
     * @param partitionIds
     *            the indices of the partitions to retain
     */
    public PLL<T> retainPartitions(List<Integer> partitionIds) {
        return new DropPartitionsPLL<T>(this, partitionIds);
    }

    /**
     * Drops the first n elements at the beginning of the collection.
     * 
     * @param n
     *            the number of elements to remove
     * @return
     */
    public PLL<T> dropFirstElements(long n) {
        Array<Long> partitionSizes = getPartitionSizes();
        long remainingToSkip = n;
        int partitionsToSkip = 0;
        while (partitionsToSkip < numPartitions() && partitionSizes.get(partitionsToSkip) < remainingToSkip) {
            remainingToSkip -= partitionSizes.get(partitionsToSkip);
            partitionsToSkip++;
        }
        List<Long> newPartitionSizes = partitionSizes.drop(partitionsToSkip).toJavaList();
        if (!newPartitionSizes.isEmpty()) {
            newPartitionSizes.set(0, newPartitionSizes.get(0) - remainingToSkip);
        }
        return new CroppedPLL<T>(this, Array.ofAll(newPartitionSizes), partitionsToSkip, remainingToSkip, false);
    }

    /**
     * Drops the last n elements at the end of the collection.
     * 
     * @param n
     *            the number of elements to remove at the end
     */
    public PLL<T> dropLastElements(long n) {
        Array<Long> partitionSizes = getPartitionSizes();
        long remainingToSkip = n;
        int partitionsToSkip = 0;
        while (partitionsToSkip < partitionSizes.size() && partitionSizes.get(numPartitions() - 1 - partitionsToSkip) < remainingToSkip) {
            remainingToSkip -= partitionSizes.get(numPartitions() - 1 - partitionsToSkip);
            partitionsToSkip++;
        }
        List<Long> newPartitionSizes = partitionSizes.dropRight(partitionsToSkip).toJavaList();
        if (!newPartitionSizes.isEmpty()) {
            newPartitionSizes.set(newPartitionSizes.size() - 1, newPartitionSizes.get(newPartitionSizes.size() - 1) - remainingToSkip);
        }
        return new CroppedPLL<T>(this, Array.ofAll(newPartitionSizes), partitionsToSkip, remainingToSkip, true);
    }

    // Memory management

    /**
     * Loads the contents of all partitions in memory.
     */
    public ProgressingFuture<Void> cacheAsync() {
        ProgressingFuture<Array<Array<T>>> partitionsFuture = collectPartitionsAsync();
        return ProgressingFutures.transform(partitionsFuture,
                partitions -> {
                    cachedPartitions = partitions;
                    return null;
                }, context.getExecutorService());
    }

    /**
     * Unloads the partition contents from memory
     */
    public void uncache() {
        cachedPartitions = null;
    }

    /**
     * Are the contents of this collection loaded in memory?
     */
    public boolean isCached() {
        return cachedPartitions != null;
    }

    /**
     * Is this PLL aware of the size of its partitions?
     */
    public boolean hasCachedPartitionSizes() {
        return cachedPartitionSizes != null;
    }

    /**
     * Sets the partition sizes if they are already known by the user.
     * 
     * @param partitionSizes
     * @return
     */
    public PLL<T> withCachedPartitionSizes(Array<Long> partitionSizes) {
        Validate.isTrue(partitionSizes.size() == numPartitions());
        cachedPartitionSizes = partitionSizes;
        return this;
    }

    // Writing out

    /**
     * Write the PLL to a directory, containing one file for each partition.
     * 
     * @param completionMarker
     *            TODO
     */
    public void saveAsTextFile(String path, int maxConcurrency, boolean repartition, boolean flushRegularly, boolean completionMarker)
            throws IOException, InterruptedException {
        ProgressingFuture<Void> future = saveAsTextFileAsync(path, maxConcurrency, repartition, flushRegularly, completionMarker);
        try {
            future.get();
        } catch (ExecutionException e) {
            throw new IOException(e.getCause());
        } catch (InterruptedException e) {
            future.cancel(true);
            throw e;
        }
    }

    /**
     * The plan of a new partition, with the boundaries defined by start and end.
     */
    private static class PlannedPartition {

        public long start;
        public long end;
        public int index;

        PlannedPartition(long start, long end, int index) {
            this.start = start;
            this.end = end;
            this.index = index;
        }
    }

    /**
     * Write the PLL to a directory, writing one file per partition, in an asynchronous way.
     *
     * @param path
     *            the directory to which to write the PLL
     * @param maxConcurrency
     *            the maximum number of partitions to write concurrently
     * @param repartition
     *            whether to re-arrange partitions to make them more balanced (merging small partitions together,
     *            splitting large partitions into smaller ones).
     * @param flushRegularly
     *            whether the file should be written to disk often (at the cost of a lower compression ratio, and more
     *            disk writes)
     * @param completionMarker
     *            whether to add an empty file as completion marker (with filename
     *            {@link Runner#COMPLETION_MARKER_FILE_NAME}).
     * @return a future for the saving process, which supports progress reporting and pausing.
     */
    public ProgressingFuture<Void> saveAsTextFileAsync(String path, int maxConcurrency, boolean repartition, boolean flushRegularly,
            boolean completionMarker) {

        File gridPath = new File(path);
        gridPath.mkdirs();

        ProgressingFuture<Array<Void>> partitionWritingFuture;
        if (repartition) {
            // Plan how to repartition the PLL
            long totalCount = count();
            List<PlannedPartition> partitions = new ArrayList<>();
            if (totalCount < context.getMinSplitRowCount() * context.getDefaultParallelism()) {
                // a single split
                partitions.add(new PlannedPartition(0L, totalCount, 0));
            } else {
                // defaultParallelism many splits, unless that makes splits too big
                long splitSize = Math.min((totalCount / context.getDefaultParallelism()) + 1, context.getMaxSplitRowCount());
                int numSplits = (int) (totalCount / splitSize);
                if (numSplits * splitSize < totalCount) {
                    numSplits++;
                }
                for (int i = 0; i != numSplits; i++) {
                    partitions.add(new PlannedPartition(splitSize * i, Math.min(splitSize * (i + 1), totalCount), i));
                }
            }

            // Create files for each partition
            touchPartitions(gridPath, partitions.size());

            // Iterate sequentially over the whole PLL.
            TaskSignalling taskSignalling = new TaskSignalling(count());
            partitionWritingFuture = new ProgressingFutureWrapper<>(context.getExecutorService().submit(() -> {
                try (CloseableIterator<T> fullIterator = blockingIterator()) {
                    try {
                        Iterator<Integer> partitionSizes = Array.ofAll(partitions)
                                .iterator()
                                .map(partition -> (int) (partition.end - partition.start));
                        Iterator<Iterator<T>> partitionChunks = fullIterator.chop(partitionSizes);
                        for (PlannedPartition plannedPartition : partitions) {
                            writePlannedPartition(plannedPartition, gridPath, partitionChunks.next(), Optional.of(taskSignalling),
                                    flushRegularly);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    Void result = null;
                    return Array.of(result);
                }
            }), taskSignalling, true);
        } else {
            // we create files for all partitions first (synchronously), to make sure the PLL can be loaded
            // immediately after returning the future
            touchPartitions(gridPath, numPartitions());
            partitionWritingFuture = runOnPartitionsAsync((p, taskSignalling) -> {
                try {
                    writeOriginalPartition(p, gridPath, Optional.of(taskSignalling), flushRegularly);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return null;
            }, maxConcurrency);
        }

        // after writing the partitions, add the completion marker
        ProgressingFuture<Void> future = ProgressingFutures.transform(
                partitionWritingFuture,
                v -> {
                    if (completionMarker) {
                        try {
                            // Write an empty file as success marker
                            File successMarker = new File(gridPath, Runner.COMPLETION_MARKER_FILE_NAME);
                            try (FileOutputStream fos = new FileOutputStream(successMarker)) {
                                Writer writer = new OutputStreamWriter(fos);
                                writer.close();
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                    return null;
                }, context.getExecutorService());
        return future;
    }

    /**
     * Ensures that there is a file for each partition. This is important to make it possible for a reader to know the
     * number of partitions ahead of time, before all partitions have been fully written. The number of partitions in a
     * PLL is not supposed to change after creation of the PLL so it would be more difficult to dynamically discover new
     * partition files as they are created.
     * 
     * @param directory
     *            the directory in which the partitions are written
     * @param partitionCount
     *            the number of partitions
     */
    protected void touchPartitions(File directory, int partitionCount) {
        try {
            for (int i = 0; i != partitionCount; i++) {
                File file = new File(directory, partitionFilename(i));
                file.createNewFile();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected void writeOriginalPartition(Partition partition, File directory, Optional<TaskSignalling> taskSignalling,
            boolean flushRegularly)
            throws IOException {
        String filename = partitionFilename(partition.getIndex());
        File partFile = new File(directory, filename);
        writePartition(partition.getIndex(), iterate(partition, IterationContext.SYNCHRONOUS_DEFAULT), partFile, taskSignalling,
                flushRegularly);
    }

    protected void writePlannedPartition(PlannedPartition partition, File directory, Iterator<T> choppedIterator,
            Optional<TaskSignalling> taskSignalling, boolean flushRegularly)
            throws IOException {
        String filename = partitionFilename(partition.index);
        File partFile = new File(directory, filename);
        CloseableIterator<T> limitedIterator = CloseableIterator.wrapping(choppedIterator);
        writePartition(partition.index, limitedIterator, partFile, taskSignalling, flushRegularly);
    }

    protected String partitionFilename(int index) {
        return String.format("part-%05d.zst", index);
    }

    protected void writePartition(int partitionIndex, CloseableIterator<T> iterator, File partFile, Optional<TaskSignalling> taskSignalling,
            boolean flushRegularly)
            throws IOException {
        try (FileOutputStream fos = new FileOutputStream(partFile);
                ZstdCompressorOutputStream gos = new ZstdCompressorOutputStream(fos); // TODO pass flushRegularly?
                                                                                      // requires finding out which
                                                                                      // level to use
                Writer writer = new OutputStreamWriter(gos);
                iterator) {

            // no need to close finalIterator because it just delegates its closing to iterator
            CloseableIterator<T> finalIterator = iterator;
            if (taskSignalling.isPresent()) {
                finalIterator = taskSignalling.get().wrapStream(iterator, 10, partitionIndex);
            }
            LastFlush lastFlush = new LastFlush();
            finalIterator.zipWithIndex().forEach(tuple -> {
                try {
                    writer.write(tuple._1.toString());
                    writer.write('\n');
                    if (flushRegularly && lastFlush.isOlderThan(1000)) {
                        writer.flush();
                        lastFlush.update();
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    // Internal functions and helpers

    /* Helper to flush output streams after some interval */
    protected static class LastFlush {

        Instant instant;

        public LastFlush() {
            update();
        }

        public void update() {
            instant = Instant.now();
        }

        public boolean isOlderThan(long milliseconds) {
            return instant.plusMillis(milliseconds).isBefore(Instant.now());
        }
    }

    /**
     * Runs a task in parallel on all partitions.
     * 
     * @param <U>
     *            return type of the function to be applied to all partitions
     * @param partitionFunction
     *            the function to be applied to all partitions
     * @param maxConcurrency
     *            the maximum number of tasks to run in parallel. Set to 0 for no limit.
     * @return
     * @throws InterruptedException
     */
    public <U> Array<U> runOnPartitions(Function<Partition, U> partitionFunction, int maxConcurrency) throws InterruptedException {
        return runOnPartitions(partitionFunction, getPartitions().iterator(), maxConcurrency);
    }

    /**
     * Runs a task in parallel on all partitions, asynchronously.
     *
     * @param <U>
     *            return type of the function to be applied to all partitions
     * @param partitionFunction
     *            the function to be applied to all partitions
     * @param maxConcurrency
     *            the maximum number of tasks to run in parallel. Set to 0 for no limit.
     * @return
     */
    public <U> ProgressingFuture<Array<U>> runOnPartitionsAsync(
            BiFunction<Partition, TaskSignalling, U> partitionFunction,
            int maxConcurrency) {
        return runOnPartitionsAsync(partitionFunction, getPartitions().iterator(), maxConcurrency);
    }

    /**
     * Same as {@link #runOnPartitions(Function, int)} but wrapping any {@link InterruptedException} in an unchecked
     * {@link PLLExecutionError}.
     * 
     * @param <U>
     * @param partitionFunction
     * @return
     */
    public <U> Array<U> runOnPartitionsWithoutInterruption(Function<Partition, U> partitionFunction) {
        try {
            return runOnPartitions(partitionFunction, 0);
        } catch (InterruptedException e) {
            throw new PLLExecutionError(e);
        }
    }

    /**
     * Run a task in parallel on a selection of partitions.
     * 
     * @param <U>
     *            return type of the function to be applied to all partitions
     * @param partitionFunction
     *            the function to be applied to all partitions
     * @param partitions
     *            the partitions to apply the function on
     * @param maxConcurrency
     *            the maximum number of tasks to run in parallel. Set to 0 for no limit.
     * @return
     * @throws InterruptedException
     */
    protected <U> Array<U> runOnPartitions(Function<Partition, U> partitionFunction,
            io.vavr.collection.Iterator<? extends Partition> partitions,
            int maxConcurrency)
            throws InterruptedException {
        try {
            return runOnPartitionsAsync(
                    (partition, signalling) -> partitionFunction.apply(partition),
                    partitions,
                    maxConcurrency).get();
        } catch (ExecutionException e) {
            throw new PLLExecutionError(e);
        }
    }

    /**
     * Run a task in parallel on a selection of partitions, asynchronously.
     *
     * @param <U>
     *            return type of the function to be applied to all partitions
     * @param partitionFunction
     *            the function to be applied to all partitions
     * @param partitions
     *            the partitions to apply the function on
     * @param maxConcurrency
     *            the maximum number of tasks to run in parallel. Set to 0 for no limit.
     */
    protected <U> ProgressingFuture<Array<U>> runOnPartitionsAsync(
            BiFunction<Partition, TaskSignalling, U> partitionFunction,
            io.vavr.collection.Iterator<? extends Partition> partitions,
            int maxConcurrency) {
        // Semaphore bounding the number of concurrently running tasks
        TaskSignalling taskSignalling = hasCachedPartitionSizes() ? new TaskSignalling(count()) : new TaskSignalling(-1);

        ListenableFuture<List<U>> listFuture;
        if (maxConcurrency == 1) {
            // special case when maxConcurrency is one: we want to execute the tasks on each partition in sequential
            // order
            listFuture = context.getExecutorService().submit(() -> {
                List<U> results = new ArrayList<>();
                partitions.forEach(partition -> {
                    results.add(partitionFunction.apply(partition, taskSignalling));
                });
                return results;
            });
        } else {
            // if the concurrency is limited, we use a semaphore to limit the number of threads working on a task
            // simultaneously
            Semaphore semaphore = (maxConcurrency > 0 && maxConcurrency < numPartitions()) ? new Semaphore(maxConcurrency) : null;
            List<ListenableFuture<U>> tasks = partitions
                    .map(partition -> context.getExecutorService().submit(() -> {
                        if (semaphore != null) {
                            semaphore.acquire();
                        }
                        try {
                            return partitionFunction.apply(partition, taskSignalling);
                        } finally {
                            if (semaphore != null) {
                                semaphore.release();
                            }
                        }
                    }))
                    .collect(Collectors.toList());
            listFuture = Futures.allAsList(tasks);
        }
        ListenableFuture<Array<U>> futureWithProgress = Futures.transform(
                listFuture,
                lists -> {
                    taskSignalling.setFullProgress();
                    return Array.ofAll(lists);
                },
                context.getExecutorService());
        return new ProgressingFutureWrapper<>(futureWithProgress, taskSignalling, hasCachedPartitionSizes());
    }

    /**
     * Same as {@link #runOnPartitions(Function, Iterator, int)} but wrapping any {@link InterruptedException} as an
     * unchecked {@link PLLExecutionError}.
     */
    protected <U> Array<U> runOnPartitionsWithoutInterruption(Function<Partition, U> partitionFunction,
            Iterator<? extends Partition> partitions) {
        try {
            return runOnPartitions(partitionFunction, partitions, 0);
        } catch (InterruptedException e) {
            throw new PLLExecutionError(e);
        }
    }

    protected PLLContext getContext() {
        return context;
    }

    /**
     * @return a numerical id for the PLL allocated by its context
     */
    public long getId() {
        return id;
    }

    /**
     * Returns the PLLs that this PLL depends on, to compute its contents. This is used for debugging purposes, to
     * display the tree of dependencies of a given PLL.
     * 
     * @see #getQueryTree()
     */
    public abstract List<PLL<?>> getParents();

    /**
     * @return a tree-based representation of the dependencies of this PLL.
     */
    public QueryTree getQueryTree() {
        QueryTree[] children = getParents().stream().map(PLL::getQueryTree).toArray(QueryTree[]::new);
        List<String> flags = new ArrayList<>();
        if (isCached()) {
            flags.add("cached");
        }
        if (cachedPartitionSizes != null) {
            flags.add("sizes");
        }
        return new QueryTree(id, name + (flags.isEmpty() ? "" : " [" + String.join(", ", flags) + "]"), children);
    }

    @Override
    public String toString() {
        return getQueryTree().toString();
    }

    public static class PLLExecutionError extends RuntimeException {

        private static final long serialVersionUID = 2301721101369058924L;

        public PLLExecutionError(Exception cause) {
            super(String.format("Execution of the PLL task failed: %s", cause.getMessage()), cause);
        }
    }

}
