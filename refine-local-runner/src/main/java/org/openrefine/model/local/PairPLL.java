
package org.openrefine.model.local;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openrefine.model.local.partitioning.Partitioner;
import org.openrefine.model.local.partitioning.CroppedPartitioner;
import org.openrefine.model.local.partitioning.LongRangePartitioner;

/**
 * Adds additional methods for PLLs of keyed collections. The supplied Partitioner determines in which partition an
 * element should be found, depending on its key.
 * 
 * @author Antonin Delpeuch
 *
 * @param <K>
 *            the type of keys in the collection
 * @param <V>
 *            the type of values in the collection
 */
public class PairPLL<K, V> extends PLL<Tuple2<K, V>> {

    protected final Optional<Partitioner<K>> partitioner;
    protected final PLL<Tuple2<K, V>> pll;

    public PairPLL(PLL<Tuple2<K, V>> pll, Optional<Partitioner<K>> partitioner) {
        super(pll.getContext());
        if (partitioner.isPresent() && partitioner.get().numPartitions() != pll.numPartitions()) {
            throw new IllegalArgumentException(
                    "The partitioner and PLL are incompatible as they do not have the same number of partitions");
        }
        this.partitioner = partitioner;
        this.pll = pll;
        this.cachedPartitionSizes = pll.cachedPartitionSizes;
    }

    protected PairPLL(PLL<Tuple2<K, V>> pll, Optional<Partitioner<K>> partitioner, List<Long> partitionSizes) {
        super(pll.getContext());
        if (partitioner.isPresent() && partitioner.get().numPartitions() != pll.numPartitions()) {
            throw new IllegalArgumentException(
                    "The partitioner and PLL are incompatible as they do not have the same number of partitions");
        }
        this.partitioner = partitioner;
        this.pll = partitionSizes == null ? pll : pll.withCachedPartitionSizes(partitionSizes);
        this.cachedPartitionSizes = partitionSizes == null ? pll.cachedPartitionSizes : partitionSizes;
    }

    /**
     * @return the partitioner used to locate elements by key
     */
    public Optional<Partitioner<K>> getPartitioner() {
        return partitioner;
    }

    // bypass local cache and make sure we are hitting that of the upstream PLL
    @Override
    public List<Long> getPartitionSizes() {
        if (cachedPartitionSizes == null) {
            cachedPartitionSizes = pll.getPartitionSizes();
        } else if (cachedPartitionSizes != null && pll.cachedPartitionSizes == null) {
            pll.cachedPartitionSizes = cachedPartitionSizes;
        }
        return cachedPartitionSizes;
    }

    /**
     * Returns a PLL of the keys contained in this collection, in the same order.
     * 
     * @return
     */
    public PLL<K> keys() {
        return this.map(Tuple2::getKey);
    }

    /**
     * Returns a PLL of the values contained in this collection, in the same order.
     * 
     * @return
     */
    public PLL<V> values() {
        return this.map(Tuple2::getValue);
    }

    /**
     * Returns a PLL obtained by mapping each element and preserving the indexing. If a partitioner is set on this PLL,
     * it will also be used by the returned PLL.
     * 
     * @param <W>
     * @param mapFunction
     * @return
     */
    public <W> PairPLL<K, W> mapValues(BiFunction<K, V, W> mapFunction) {
        PLL<Tuple2<K, W>> mapped = pll.map(
                tuple -> new Tuple2<>(tuple.getKey(), mapFunction.apply(tuple.getKey(), tuple.getValue())));
        return new PairPLL<K, W>(mapped, partitioner);
    }

    /**
     * Returns the list of elements of the PLL indexed by the given key. This operation will be more efficient if a
     * partitioner is available, making it possible to scan the relevant partition only.
     * 
     * @param key
     * @return
     */
    public List<V> get(K key) {
        if (partitioner.isPresent()) {
            int partitionId = partitioner.get().getPartition(key);
            Partition partition = getPartitions().get(partitionId);
            Stream<Tuple2<K, V>> stream = iterate(partition);
            return stream.filter(tuple -> key.equals(tuple.getKey()))
                    .map(Tuple2::getValue).collect(Collectors.toList());
        } else {
            return filter(tuple -> key.equals(tuple.getKey())).values().collect();
        }
    }

    /**
     * Returns the list of elements starting at the given key and for up to the given number of elements. This assumes
     * that the PLL is sorted by keys.
     * 
     * @param from
     *            the first key to return
     * @param limit
     *            the maximum number of elements to return
     * @param comparator
     *            the ordering on K that is assumed on the PLL
     * @return
     */
    public List<Tuple2<K, V>> getRangeAfter(K from, int limit, Comparator<K> comparator) {
        Stream<Tuple2<K, V>> stream = streamFromKey(from, comparator);
        return stream
                .limit(limit).collect(Collectors.toList());
    }

    /**
     * Returns the list of elements ending at the given key (excluded) and for up to the given number of elements. This
     * assumes that the PLL is sorted by keys.
     *
     * @param upperBound
     *            the least key not to return
     * @param limit
     *            the maximum number of elements to return
     * @param comparator
     *            the ordering on K that is assumed on the PLL
     * @return
     *
     *         TODO this could be optimized further, when the partitions are cached in memory, we can iterate from them
     *         in reverse
     */
    public List<Tuple2<K, V>> getRangeBefore(K upperBound, int limit, Comparator<K> comparator) {
        Stream<Tuple2<K, V>> stream;
        if (partitioner.isEmpty() || !(partitioner.get() instanceof LongRangePartitioner)) {
            // we resort to simple scanning of all partitions
            return gatherElementsBefore(upperBound, limit, stream(), comparator);
        } else {
            // we can use the partitioner to locate the partition to end at
            int endPartition = partitioner.get().getPartition(upperBound);
            List<Tuple2<K, V>> result = new ArrayList<>(limit);
            for (int currentPartition = endPartition; currentPartition >= 0 && result.size() != limit; currentPartition--) {
                List<Tuple2<K, V>> lastElements = gatherElementsBefore(upperBound, limit, iterate(getPartitions().get(currentPartition)),
                        comparator);
                for (int i = lastElements.size() - 1; i >= 0 && result.size() < limit; i--) {
                    result.add(lastElements.get(i));
                }
            }
            Collections.reverse(result);
            return result;
        }
    }

    /**
     * Returns the last n elements whose key is strictly less than the supplied upper bound.
     *
     * @param stream
     *            the stream to take the elements from, which is assumed to be in increasing order
     */
    protected static <K, V> List<Tuple2<K, V>> gatherElementsBefore(K upperBound, int limit, Stream<Tuple2<K, V>> stream,
            Comparator<K> comparator) {
        Deque<Tuple2<K, V>> lastElements = new ArrayDeque<>(limit);
        stream.takeWhile(tuple -> comparator.compare(upperBound, tuple.getKey()) > 0)
                .forEach(tuple -> {
                    if (lastElements.size() == limit) {
                        lastElements.removeFirst();
                    }
                    lastElements.addLast(tuple);
                });
        return new ArrayList<>(lastElements);
    }

    /**
     * Returns the list of elements whose keys match one of the supplied keys.
     * 
     * @param keys
     *            the keys to look up
     * @return the list of elements in the order they appear in the PLL
     */
    public List<Tuple2<K, V>> getByKeys(Set<K> keys) {
        if (partitioner.isEmpty() || !(partitioner.get() instanceof LongRangePartitioner)) {
            return this.filter(t -> keys.contains(t.getKey())).collect();
        } else {
            // if the PLL is sorted by keys then we can only scan the partitions
            // where the keys would go, and stop scanning those partitions as soon
            // as a greater element is found
            LongRangePartitioner sortedPartitioner = (LongRangePartitioner) partitioner.get();
            Set<Long> longKeys = keys.stream().map(k -> (Long) k).collect(Collectors.toSet());

            // Compute the largest key to look for in each partition
            Map<Integer, Long> maxKey = new HashMap<>();
            for (Long key : longKeys) {
                int partitionId = sortedPartitioner.getPartition(key);
                long newMax = key;
                if (maxKey.containsKey(partitionId)) {
                    newMax = Math.max(maxKey.get(partitionId), key);
                }
                maxKey.put(partitionId, newMax);
            }

            // Stream in each partition where we could find any of the supplied keys
            List<List<Tuple2<K, V>>> partitionResults = runOnPartitionsWithoutInterruption(
                    partition -> {
                        long max = maxKey.get(partition.getIndex());
                        return iterate(partition)
                                .takeWhile(tuple -> (long) tuple.getKey() <= max)
                                .filter(tuple -> longKeys.contains(tuple.getKey()))
                                .collect(Collectors.toList());
                    },
                    getPartitions().stream()
                            .filter(partition -> maxKey.containsKey(partition.getIndex())));

            return partitionResults.stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Iterates over the elements of this PLL starting from a given key (inclusive). This assumes that the PLL is
     * sorted.
     * 
     * @param from
     *            the first key to start iterating from
     * @param comparator
     *            the order used to compare the keys
     * @return a streams which starts on the first element whose key is greater or equal to the provided one
     */
    public Stream<Tuple2<K, V>> streamFromKey(K from, Comparator<K> comparator) {
        Stream<Tuple2<K, V>> stream;
        if (partitioner.isEmpty() || !(partitioner.get() instanceof LongRangePartitioner)) {
            // we resort to simple scanning of all partitions
            stream = stream();
        } else {
            // we can use the partitioner to locate the partition to start from
            int startingPartition = partitioner.get().getPartition(from);
            stream = streamFromPartition(startingPartition);
        }
        return stream.dropWhile(tuple -> comparator.compare(from, tuple.getKey()) > 0);
    }

    /**
     * Iterates over the elements of this PLL up to the given key (exclusive). This assumes that the PLL is sorted.
     * 
     * @param upTo
     *            the key to stop iterating at
     * @param comparator
     *            the order used to compare the keys
     * @return
     */
    public Stream<Tuple2<K, V>> streamUpToKey(K upTo, Comparator<K> comparator) {
        return stream().takeWhile(tuple -> comparator.compare(tuple.getKey(), upTo) < 0);
    }

    /**
     * Iterates over the elements of this PLL from the given key (inclusive) and up to the other given key (exclusive).
     * This assumes that the PLL is sorted.
     * 
     * @param from
     *            the key to start iterating from
     * @param upTo
     *            the key to stop iterating at
     * @param comparator
     *            the order used to compare the keys
     * @return
     */
    public Stream<Tuple2<K, V>> streamBetweenKeys(K from, K upTo, Comparator<K> comparator) {
        return streamFromKey(from, comparator)
                .takeWhile(tuple -> comparator.compare(tuple.getKey(), upTo) < 0);
    }

    /**
     * Iterates over the elements of this PLL between the given keys, where both boundaries are optional. This assumes
     * that the PLL is sorted.
     * 
     * @param from
     * @param upTo
     * @param comparator
     * @return
     */
    public Stream<Tuple2<K, V>> streamBetweenKeys(Optional<K> from, Optional<K> upTo, Comparator<K> comparator) {
        if (from.isEmpty() && upTo.isEmpty()) {
            return stream();
        } else if (from.isEmpty() && upTo.isPresent()) {
            return streamUpToKey(upTo.get(), comparator);
        } else if (from.isPresent() && upTo.isEmpty()) {
            return streamFromKey(from.get(), comparator);
        } else {
            return streamBetweenKeys(from.get(), upTo.get(), comparator);
        }
    }

    @Override
    public PairPLL<K, V> filter(Predicate<? super Tuple2<K, V>> predicate) {
        return new PairPLL<K, V>(pll.filter(predicate), partitioner);
    }

    @Override
    public PairPLL<K, V> limitPartitions(long limit) {
        return new PairPLL<K, V>(pll.limitPartitions(limit), partitioner);
    }

    /**
     * Drops the first n elements at the beginning of the collection. This also adapts any partitioner to work on the
     * cropped collection.
     * 
     * @param n
     *            the number of elements to remove
     * @return
     */
    @Override
    public PairPLL<K, V> dropFirstElements(long n) {
        PLL<Tuple2<K, V>> croppedPLL = pll.dropFirstElements(n);
        Optional<Partitioner<K>> newPartitioner = Optional.empty();
        if (partitioner.isPresent()) {
            newPartitioner = Optional
                    .of(CroppedPartitioner.crop(partitioner.get(), pll.numPartitions() - croppedPLL.numPartitions(), false));
        }
        return new PairPLL<K, V>(croppedPLL, newPartitioner);
    }

    /**
     * Drops the first n elements at the end of the collection. This also adapts any partitioner to work on the cropped
     * collection.
     * 
     * @param n
     *            the number of elements to remove
     * @return
     */
    @Override
    public PairPLL<K, V> dropLastElements(long n) {
        PLL<Tuple2<K, V>> croppedPLL = pll.dropLastElements(n);
        Optional<Partitioner<K>> newPartitioner = Optional.empty();
        if (partitioner.isPresent()) {
            newPartitioner = Optional
                    .of(CroppedPartitioner.crop(partitioner.get(), pll.numPartitions() - croppedPLL.numPartitions(), true));
        }
        return new PairPLL<K, V>(croppedPLL, newPartitioner);
    }

    public PairPLL<K, V> concatenate(PairPLL<K, V> other) {
        return new PairPLL<K, V>(pll.concatenate(other), Optional.empty());
    }

    @Override
    protected Stream<Tuple2<K, V>> compute(Partition partition) {
        return pll.compute(partition);
    }

    @Override
    public Stream<Tuple2<K, V>> iterate(Partition partition) {
        // Overridden to ensure we are only caching this PLL once, in the parent PLL
        return pll.iterate(partition);
    }

    @Override
    public List<? extends Partition> getPartitions() {
        return pll.getPartitions();
    }

    /**
     * Returns the underlying PLL, discarding any partitioner.
     */
    public PLL<Tuple2<K, V>> toPLL() {
        return pll;
    }

    /**
     * Assuming that the keys of the PairPLL are indices, deduce the partition sizes from the first element of each
     * partition and the total number of elements, creating an appropriate RangePartitioner and adding it to the PLL.
     * 
     * If the total row count is unknown (negative) then partition sizes are not inferred.
     * 
     * @param pairPLL
     *            the new PLL with the derived partitioner
     * @param totalRowCount
     *            the known row count
     * @return
     */
    public static <T> PairPLL<Long, T> assumeIndexed(PairPLL<Long, T> pairPLL, long totalRowCount) {
        if (pairPLL.getPartitioner().isPresent()) {
            return pairPLL;
        }
        PLL<Long> keys = pairPLL.keys();
        List<Long> firstKeys = keys.runOnPartitionsWithoutInterruption(p -> keys.iterate(p).findFirst())
                .stream()
                .map(optional -> optional.isPresent() ? optional.get() : null)
                .collect(Collectors.toList());
        Partitioner<Long> partitioner = new LongRangePartitioner(keys.numPartitions(), firstKeys.subList(1, keys.numPartitions()));
        if (totalRowCount >= 0) {
            // Derive the size of each partition
            List<Long> partitionSizes = new ArrayList<>();
            long lastIndexSeen = totalRowCount;
            for (int i = keys.numPartitions() - 1; i != -1; i--) {
                if (firstKeys.get(i) == null) {
                    partitionSizes.add(0L);
                } else {
                    long firstKey = firstKeys.get(i);
                    partitionSizes.add(lastIndexSeen - firstKey);
                    lastIndexSeen = firstKey;
                }
            }
            Collections.reverse(partitionSizes);
            return new PairPLL<Long, T>(pairPLL, Optional.of(partitioner), partitionSizes);
        } else {
            return new PairPLL<Long, T>(pairPLL, Optional.of(partitioner));
        }
    }

    /**
     * Assumes that a PLL is sorted by key and derive the appropriate partitioner for it.
     * 
     * @param <T>
     * @param pairPLL
     * @return
     */
    public static <T> PairPLL<Long, T> assumeSorted(PairPLL<Long, T> pairPLL) {
        if (pairPLL.getPartitioner().isPresent()) {
            return pairPLL;
        }
        PLL<Long> keys = pairPLL.keys();
        List<Long> firstKeys = keys.runOnPartitionsWithoutInterruption(p -> keys.iterate(p).findFirst())
                .stream()
                .map(optional -> optional.isEmpty() ? null : optional.get())
                .skip(1)
                .collect(Collectors.toList());
        Partitioner<Long> partitioner = new LongRangePartitioner(keys.numPartitions(), firstKeys);
        return new PairPLL<Long, T>(pairPLL, Optional.of(partitioner));

    }

    /**
     * Returns a copy of this PairPLL with a changed partitioner.
     * 
     * @param partitioner
     * @return
     */
    public PairPLL<K, V> withPartitioner(Optional<Partitioner<K>> partitioner) {
        return new PairPLL<K, V>(pll, partitioner);
    }

    /**
     * Returns a copy of this PairPLL with the given partition sizes, when they are externally known.
     */
    public PairPLL<K, V> withCachedPartitionSizes(List<Long> newCachedPartitionSizes) {
        return new PairPLL<K, V>(pll, partitioner, newCachedPartitionSizes);
    }

    /**
     * Assuming both PairPLLs are ordered by key, and each key appears at most once in each dataset, returns an ordered
     * PairPLL with the inner join of both PLLs.
     */
    public <W> PairPLL<K, Tuple2<V, W>> innerJoinOrdered(PairPLL<K, W> other, Comparator<K> comparator) {
        OrderedJoinPLL<K, V, W> joined = new OrderedJoinPLL<K, V, W>(this, other, comparator, true);
        return new PairPLL<K, Tuple2<V, W>>(joined, joined.getPartitioner());
    }

    /**
     * Assuming both PairPLLs are ordered by key, and each key appears at most once in each dataset, returns an ordered
     * PairPLL with the outer join of both PLLs.
     */
    public <W> PairPLL<K, Tuple2<V, W>> outerJoinOrdered(PairPLL<K, W> other, Comparator<K> comparator) {
        OrderedJoinPLL<K, V, W> joined = new OrderedJoinPLL<K, V, W>(this, other, comparator, false);
        return new PairPLL<K, Tuple2<V, W>>(joined, joined.getPartitioner());
    }

}
