
package org.openrefine.model.local;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openrefine.model.local.partitioning.LongRangePartitioner;
import org.openrefine.model.local.partitioning.Partitioner;

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
    }

    protected PairPLL(PLL<Tuple2<K, V>> pll, Optional<Partitioner<K>> partitioner, List<Long> partitionSizes) {
        super(pll.getContext());
        if (partitioner.isPresent() && partitioner.get().numPartitions() != pll.numPartitions()) {
            throw new IllegalArgumentException(
                    "The partitioner and PLL are incompatible as they do not have the same number of partitions");
        }
        this.partitioner = partitioner;
        this.pll = pll;
        this.cachedPartitionSizes = partitionSizes;
    }

    /**
     * @return the partitioner used to locate elements by key
     */
    public Optional<Partitioner<K>> getPartitioner() {
        return partitioner;
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
        return new PairPLL<K, W>(mapped, partitioner, cachedPartitionSizes);
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
     * that the PLL is sorted by keys.
     * 
     * @param from
     *            the first key to return
     * @param limit
     *            the maximum number of elements to return
     * @param comparator
     *            the ordering on K that is assumed on the PLL
     * @return
     */
    public List<Tuple2<K, V>> getRange(K from, int limit, Comparator<K> comparator) {
        Stream<Tuple2<K, V>> stream = streamFromKey(from, comparator);
        return stream
                .limit(limit).collect(Collectors.toList());
    }

    /**
     * Iterates over the elements of this PLL starting from a given key (inclusive). This assumes that the PLL is
     * sorted.
     * 
     * @param from
     *            the first key to start iterating from
     * @param comparator
     *            the order used to compare the keys
     * @return
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
     * If the total row count is unknown (negative) then the more expensive equivalent is used:
     * pairPLL.values().zipWithIndex().
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
        return new PairPLL<K, V>(pll, partitioner, cachedPartitionSizes);
    }

    /**
     * Returns a copy of this PairPLL with the given partition sizes, when they are externally known.
     */
    public PairPLL<K, V> withCachedPartitionSizes(List<Long> newCachedPartitionSizes) {
        if (newCachedPartitionSizes.size() != pll.numPartitions()) {
            throw new IllegalArgumentException("Invalid number of partition sizes provided");
        }
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

    /*
     * public void saveAsHadoopFile(String path, Class<K> keyClass, Class<V> valueClass, OutputFormat<K, V>
     * outputFormat, Class<? extends CompressionCodec> codec) throws IOException { Job job =
     * Job.getInstance(context.getFileSystem().getConf()); job.setOutputKeyClass(keyClass);
     * job.setOutputValueClass(valueClass); job.setOutputFormatClass(outputFormat.getClass()); Configuration jobConf =
     * job.getConfiguration(); jobConf.set("mapreduce.output.fileoutputformat.outputdir", path); if (codec != null) {
     * jobConf.set("mapreduce.output.fileoutputformat.compress", "true");
     * jobConf.set("mapreduce.output.fileoutputformat.compress.codec", codec.getCanonicalName());
     * jobConf.set("mapreduce.output.fileoutputformat.compress.type", CompressionType.BLOCK.toString()); }
     * 
     * // Create the committer OutputCommitter committer = outputFormat.getOutputCommitter(attemptContext); JobContext
     * jobContext; Configuration jobContextConfig = jobContext.getConfiguration();
     * jobContextConfig.set("mapreduce.job.id", jobId.toString()); jobContextConfig.set("mapreduce.task.id",
     * attemptId.getTaskID().toString()); jobContextConfig.set("mapreduce.task.attempt.id", attemptId.toString());
     * jobContextConfig.setBoolean("mapreduce.task.ismap", true); committer.setupJob(jobContext);
     * 
     * runOnPartitions(writePartition(jobConf, outputFormat.getClass())); }
     * 
     * protected Function<Partition, Long> writePartition(Configuration jobConf, Class<? extends OutputFormat> class1) {
     * return (partition -> { // Derive the filename used to serialize this partition NumberFormat formatter =
     * NumberFormat.getInstance(Locale.US); formatter.setMinimumIntegerDigits(5); formatter.setGroupingUsed(false);
     * String outputName = "part-" + formatter.format(partition.getIndex());
     * 
     * // Create the task attempt context int jobId = 0; TaskAttemptID attemptId = new TaskAttemptID("", jobId,
     * TaskType.REDUCE, partition.getIndex(), 0); Configuration attemptConf = new Configuration(jobConf);
     * attemptConf.setInt("mapreduce.task.partition", partition.getIndex()); TaskAttemptContext attemptContext = new
     * TaskAttemptContextImpl(attemptConf, attemptId);
     * 
     * try { // Instantiate the output format OutputFormat<K, V> outputFormat = class1.newInstance(); if (outputFormat
     * instanceof Configurable) { ((Configurable) outputFormat).setConf(attemptConf); }
     * 
     * 
     * 
     * 
     * // Initialize the writer RecordWriter<K,V> writer = outputFormat.getRecordWriter(attemptContext);
     * iterate(partition).forEach(tuple -> { try { writer.write(tuple.getKey(), tuple.getValue()); } catch (IOException
     * | InterruptedException e) { throw new UncheckedExecutionException(e); } finally { try {
     * writer.close(attemptContext); } catch (IOException | InterruptedException e) { throw new
     * UncheckedExecutionException(e); } } }); } catch (IOException | InterruptedException | InstantiationException |
     * IllegalAccessException e) { throw new UncheckedExecutionException(e); } return 0L; }); }
     */

}
