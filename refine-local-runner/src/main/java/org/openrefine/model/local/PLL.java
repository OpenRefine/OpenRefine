
package org.openrefine.model.local;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;

import org.openrefine.model.Grid;
import org.openrefine.model.local.util.QueryTree;
import org.openrefine.process.ProgressReporter;

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
    protected List<Long> cachedPartitionSizes;
    // cached contents of each partition, initialized on demand
    protected List<List<T>> cachedPartitions;

    public PLL(PLLContext context, String name) {
        Validate.notNull(context);
        this.context = context;
        this.cachedPartitionSizes = null;
        this.cachedPartitions = null;
        this.id = context.allocateId();
        this.name = name;
    }

    /**
     * Iterate over the elements of the given partition. This is the method that should implemented by subclasses. As
     * this method forces computation, ignoring any caching, consumers should not call it directly but rather use
     * {@link #iterate(Partition)}.
     * 
     * @param partition
     *            the partition to iterate over
     * @return
     */
    protected abstract Stream<T> compute(Partition partition);

    /**
     * @return the number of partitions in this list
     */
    public int numPartitions() {
        return getPartitions().size();
    }

    /**
     * @return the partitions in this list
     */
    public abstract List<? extends Partition> getPartitions();

    /**
     * Iterate over the elements of the given partition. If the contents of this PLL have been cached, this will iterate
     * from the cache instead.
     * 
     * @param partition
     *            the partition to iterate over
     * @return
     */
    public Stream<T> iterate(Partition partition) {
        if (cachedPartitions != null) {
            return cachedPartitions.get(partition.getIndex()).stream();
        } else {
            return compute(partition);
        }
    }

    /**
     * @return the total number of elements
     */
    public long count() {
        return getPartitionSizes()
                .stream()
                .mapToLong(Long::longValue)
                .sum();
    }

    /**
     * @return the number of elements in each partition
     */
    public List<Long> getPartitionSizes() {
        if (cachedPartitionSizes == null) {
            cachedPartitionSizes = runOnPartitionsWithoutInterruption(p -> iterate(p).count());
        }
        return cachedPartitionSizes;
    }

    /**
     * @return the list of all elements in the list, retrieved in memory.
     */
    public List<T> collect() {
        List<List<T>> partitionLists = collectPartitions(Optional.empty());

        return partitionLists.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * @return the list of all elements in this collection, retrieved in an ArrayList
     */
    protected ArrayList<T> collectToArrayList() {
        List<List<T>> partitionLists = collectPartitions(Optional.empty());
        int size = partitionLists.stream().mapToInt(p -> p.size()).sum();

        return partitionLists.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(() -> new ArrayList<T>(size)));
    }

    /**
     * Retrieves the contents of all partitions. This does not store them in the local cache, so two successive calls to
     * this method will enumerate the contents of the PLL twice.
     * 
     * @param progressReporter
     *            reports the progress of computing the whole contents. If the sizes of the partitions are not known,
     *            progress will jump from 0 to 100 directly
     */
    protected List<List<T>> collectPartitions(Optional<ProgressReporter> progressReporter) {
        List<List<T>> results;
        if (cachedPartitions != null) {
            results = cachedPartitions;
        } else {
            if (progressReporter.isPresent() && cachedPartitionSizes != null) {
                ConcurrentProgressReporter concurrentReporter = new ConcurrentProgressReporter(progressReporter.get(), count());
                results = runOnPartitionsWithoutInterruption(p -> concurrentReporter.wrapStream(
                        iterate(p),
                        100, // number of elements each thread reads before updating progress
                        p.getIndex() * 5) // offset which depends on the partition index so that not all threads report
                                          // progress at the same time
                        .collect(Collectors.toList()));
            } else {
                results = runOnPartitionsWithoutInterruption(p -> iterate(p).collect(Collectors.toList()));
            }
        }

        if (progressReporter.isPresent()) {
            progressReporter.get().reportProgress(100);
        }

        if (cachedPartitionSizes == null) {
            cachedPartitionSizes = results
                    .stream()
                    .map(l -> (long) l.size())
                    .collect(Collectors.toList());
        }
        return results;
    }

    /**
     * Returns an iterator over the list
     */
    public Stream<T> stream() {
        return streamFromPartition(0);
    }

    /**
     * Stream over the part of the collection that starts at given partition boundary.
     * 
     * @param partitionId
     *            the index of the partition to start enumerating from
     * @return
     */
    protected Stream<T> streamFromPartition(int partitionId) {
        Stream<? extends Partition> partitions = getPartitions().stream().skip(partitionId);
        if (cachedPartitions != null) {
            return partitions
                    .flatMap(p -> cachedPartitions.get(p.getIndex()).stream());
        } else {
            return partitions
                    .flatMap(p -> iterate(p));
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
            return !stream().iterator().hasNext();
        }
    }

    /**
     * Returns the n first elements of the list (or less if there are less elements in the list).
     */
    public List<T> take(int num) {
        if (num < 0) {
            throw new IllegalArgumentException("The number of elements to take must be non-negative");
        }
        return stream().limit(num).collect(Collectors.toList());
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
        List<U> states = runOnPartitionsWithoutInterruption(partition -> aggregateStream(initialValue, map, combine, iterate(partition)));
        return aggregateStream(initialValue, combine, combine, states.stream());
    }

    private static <U, T> U aggregateStream(U initialValue, BiFunction<U, T, U> map, BiFunction<U, U, U> combine, Stream<T> iterator) {
        return iterator.reduce(initialValue, (u, t) -> map.apply(u, t), (u1, u2) -> combine.apply(u1, u2));
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
        BiFunction<Integer, Stream<T>, Stream<U>> partitionMap = ((i, parentIterator) -> parentIterator.map(mapFunction));
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
    public <U> PLL<U> flatMap(Function<T, Stream<U>> mapFunction, String mapDescription) {
        BiFunction<Integer, Stream<T>, Stream<U>> partitionMap = ((i, parentStream) -> parentStream.flatMap(mapFunction));
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
        return mapPartitions((i, parentStream) -> batchStream(parentStream, batchSize),
                String.format("Group into batches of %d elements", batchSize),
                batchSize == 1);
    }

    protected static <T> Stream<List<T>> batchStream(Stream<T> stream, int batchSize) {
        Iterator<T> parentIterator = stream.iterator();
        Iterator<List<T>> iterator = Iterators.partition(parentIterator, batchSize);
        return Streams.stream(iterator)
                .onClose(() -> stream.close());
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
            BiFunction<Integer, Stream<T>, Stream<U>> map,
            String mapDescription,
            boolean preservesSizes) {
        if (preservesSizes) {
            return new MapPartitionsPLL<T, U>(this, map, mapDescription, cachedPartitionSizes);
        } else {
            return new MapPartitionsPLL<T, U>(this, map, mapDescription);
        }
    }

    /**
     * Applies a map function on the list, such that the map function is able to keep a state from one element to the
     * other. This state is required to be combineable with an associative and unital function.
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
        List<? extends Partition> partitions = getPartitions();
        List<S> partitionStates = runOnPartitionsWithoutInterruption(partition -> aggregateStream(
                initialState,
                (s, t) -> combine.apply(s, feed.apply(t)), combine, iterate(partition)),
                partitions.stream().limit(partitions.size() - 1));
        List<S> initialStates = new ArrayList<>(numPartitions());
        S currentState = initialState;
        for (int i = 0; i != numPartitions(); i++) {
            initialStates.add(currentState);
            if (i < partitionStates.size()) {
                currentState = combine.apply(currentState, partitionStates.get(i));
            }
        }

        BiFunction<Integer, Stream<T>, Stream<U>> partitionMap = ((i, stream) -> scanMapStream(stream, initialStates.get(i), feed, combine,
                map));
        return mapPartitions(partitionMap, "scan map", true);
    }

    protected static <T, S, U> Stream<U> scanMapStream(
            Stream<T> stream,
            S initialState,
            Function<T, S> feed,
            BiFunction<S, S, S> combine,
            BiFunction<S, T, U> map) {
        Iterator<T> iterator = stream.iterator();
        return Streams.stream(new Iterator<U>() {

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

        });
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
        BiFunction<Integer, Stream<T>, Stream<T>> partitionMap = ((i, parentIterator) -> parentIterator.filter(filterPredicate));
        return mapPartitions(partitionMap, "filter", false);
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
        List<T> sorted = collectToArrayList();
        sorted.sort(comparator);
        return new InMemoryPLL<T>(context, sorted, numPartitions());
    }

    /**
     * Concatenates another PLL at the end of this one, resulting in a new PLL. The new PLL has the union of the
     * partitions of both original PLLs as partition set.
     * 
     * @param other
     *            the list of elements to add at the end of this one
     * @return
     */
    public PLL<T> concatenate(PLL<T> other) {
        return new UnionPLL<T>(this, other);
    }

    /**
     * Limit each partition to contain only their first N elements.
     * 
     * @param limit
     *            the maximum number of items per partition
     * @return
     */
    public PLL<T> limitPartitions(long limit) {
        List<Long> newCachedPartitionSizes = null;
        if (cachedPartitionSizes != null) {
            newCachedPartitionSizes = cachedPartitionSizes.stream()
                    .map(size -> Math.min(size, limit))
                    .collect(Collectors.toList());
        }
        BiFunction<Integer, Stream<T>, Stream<T>> map = ((i, stream) -> stream.limit(limit));
        return new MapPartitionsPLL<T, T>(this, map, String.format("Limit each partition to %d", limit), newCachedPartitionSizes);
    }

    /**
     * Drops the first n elements at the beginning of the collection.
     * 
     * @param n
     *            the number of elements to remove
     * @return
     */
    public PLL<T> dropFirstElements(long n) {
        List<Long> partitionSizes = getPartitionSizes();
        long remainingToSkip = n;
        int partitionsToSkip = 0;
        while (partitionsToSkip < numPartitions() && partitionSizes.get(partitionsToSkip) < remainingToSkip) {
            remainingToSkip -= partitionSizes.get(partitionsToSkip);
            partitionsToSkip++;
        }
        List<Long> newPartitionSizes = new ArrayList<>(partitionSizes.subList(partitionsToSkip, partitionSizes.size()));
        if (!newPartitionSizes.isEmpty()) {
            newPartitionSizes.set(0, newPartitionSizes.get(0) - remainingToSkip);
        }
        return new CroppedPLL<T>(this, newPartitionSizes, partitionsToSkip, remainingToSkip, false);
    }

    /**
     * Drops the last n elements at the end of the collection.
     * 
     * @param n
     *            the number of elements to remove at the end
     */
    public PLL<T> dropLastElements(long n) {
        List<Long> partitionSizes = getPartitionSizes();
        long remainingToSkip = n;
        int partitionsToSkip = 0;
        while (partitionsToSkip < partitionSizes.size() && partitionSizes.get(numPartitions() - 1 - partitionsToSkip) < remainingToSkip) {
            remainingToSkip -= partitionSizes.get(numPartitions() - 1 - partitionsToSkip);
            partitionsToSkip++;
        }
        List<Long> newPartitionSizes = new ArrayList<>(partitionSizes.subList(0, partitionSizes.size() - partitionsToSkip));
        if (!newPartitionSizes.isEmpty()) {
            newPartitionSizes.set(newPartitionSizes.size() - 1, newPartitionSizes.get(newPartitionSizes.size() - 1) - remainingToSkip);
        }
        return new CroppedPLL<T>(this, newPartitionSizes, partitionsToSkip, remainingToSkip, true);
    }

    // Memory management

    /**
     * Loads the contents of all partitions in memory.
     */
    public void cache(Optional<ProgressReporter> progressReporter) {
        cachedPartitions = collectPartitions(progressReporter);
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
    public PLL<T> withCachedPartitionSizes(List<Long> partitionSizes) {
        Validate.isTrue(partitionSizes.size() == numPartitions());
        cachedPartitionSizes = partitionSizes;
        return this;
    }

    // Writing out

    /**
     * Write the PLL to a directory, containing one file for each partition.
     * 
     * @param path
     * @param progressReporter
     *            optionally reports progress of the write operation
     * @throws IOException
     * @throws InterruptedException
     */
    public void saveAsTextFile(String path, Optional<ProgressReporter> progressReporter) throws IOException, InterruptedException {
        // Using the Hadoop API:
        /*
         * OutputFormat<NullWritable, Text> outputFormat = new TextOutputFormat<NullWritable, Text>();
         * mapPartitions((index,stream) -> { Text text = new Text(); return stream.map(e -> {text.set(e.toString());
         * return text; }); }, false) .mapToPair(text -> Tuple2.of(NullWritable.get(), text)) .saveAsHadoopFile(path,
         * NullWritable.class, Text.class, outputFormat, GzipCodec.class);
         */
        File gridPath = new File(path);
        gridPath.mkdirs();

        Optional<ConcurrentProgressReporter> concurrentProgressReporter = ((progressReporter.isPresent() && cachedPartitionSizes != null)
                ? Optional.of(new ConcurrentProgressReporter(progressReporter.get(), count()))
                : Optional.empty());

        try {
            runOnPartitions(p -> {
                try {
                    writePartition(p, gridPath, concurrentProgressReporter);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return false;
            });
        } catch (InterruptedException e) {
            // if the operation was interrupted, we remove all the files we were writing
            FileUtils.deleteDirectory(gridPath);
            throw e;
        }

        if (progressReporter.isPresent()) {
            // to make sure we reach the end even if the partition sizes have not been computed yet
            progressReporter.get().reportProgress(100);
        }

    }

    protected void writePartition(Partition partition, File directory, Optional<ConcurrentProgressReporter> progressReporter)
            throws IOException {
        String filename = String.format("part-%05d.gz", partition.getIndex());
        File partFile = new File(directory, filename);
        FileOutputStream fos = null;
        GZIPOutputStream gos = null;
        try {
            fos = new FileOutputStream(partFile);
            gos = new GZIPOutputStream(fos);
            Writer writer = new OutputStreamWriter(gos);
            Stream<T> stream = iterate(partition);
            if (progressReporter.isPresent()) {
                stream = progressReporter.get().wrapStream(stream, 10, partition.getIndex());
            }
            stream.forEachOrdered(row -> {
                try {
                    writer.write(row.toString());
                    writer.write('\n');
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            writer.close();
        } finally {
            if (gos != null) {
                gos.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    // Internal functions

    /**
     * Runs a task in parallel on all partitions.
     * 
     * @param <U>
     *            return type of the function to be applied to all partitions
     * @param partitionFunction
     *            the function to be applied to all partitions
     * @return
     * @throws InterruptedException
     */
    public <U> List<U> runOnPartitions(Function<Partition, U> partitionFunction) throws InterruptedException {
        return runOnPartitions(partitionFunction, getPartitions().stream());
    }

    /**
     * Same as {@link #runOnPartitions(Function)} but wrapping any {@link InterruptedException} in an unchecked
     * {@link PLLExecutionError}.
     * 
     * @param <U>
     * @param partitionFunction
     * @return
     */
    public <U> List<U> runOnPartitionsWithoutInterruption(Function<Partition, U> partitionFunction) {
        try {
            return runOnPartitions(partitionFunction);
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
     * @return
     * @throws InterruptedException
     */
    protected <U> List<U> runOnPartitions(Function<Partition, U> partitionFunction, Stream<? extends Partition> partitions)
            throws InterruptedException {
        List<ListenableFuture<U>> tasks = partitions
                .map(partition -> context.getExecutorService().submit(() -> partitionFunction.apply(partition)))
                .collect(Collectors.toList());
        ListenableFuture<List<U>> listFuture = Futures.allAsList(tasks);
        try {
            return listFuture.get();
        } catch (InterruptedException e) {
            listFuture.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            listFuture.cancel(true);
            throw new PLLExecutionError(e);
        }
    }

    /**
     * Same as {@link #runOnPartitions(Function, Stream)} but wrapping any {@link InterruptedException} as an unchecked
     * {@link PLLExecutionError}.
     * 
     * @param <U>
     * @param partitionFunction
     * @param partitions
     * @return
     */
    protected <U> List<U> runOnPartitionsWithoutInterruption(Function<Partition, U> partitionFunction,
            Stream<? extends Partition> partitions) {
        try {
            return runOnPartitions(partitionFunction, partitions);
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
        return new QueryTree(id, name + (isCached() ? " [cached]" : ""), children);
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
