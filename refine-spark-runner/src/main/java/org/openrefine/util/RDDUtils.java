
package org.openrefine.util;

import java.util.*;

import com.google.common.collect.Iterators;
import org.apache.spark.Partitioner;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;
import scala.reflect.ClassManifestFactory;
import scala.reflect.ClassTag;

import org.openrefine.model.IndexedRow;
import org.openrefine.model.Row;
import org.openrefine.model.rdd.PartitionedRDD;
import org.openrefine.model.rdd.SortedRDD.SortedPartitioner;
import org.openrefine.model.rdd.ZippedWithIndexRDD;

/**
 * Collection of utilities around Spark RDDs.
 * 
 * @author Antonin Delpeuch
 *
 */
public class RDDUtils {

    @SuppressWarnings("unchecked")
    public static final ClassTag<Tuple2<Long, Row>> ROW_TUPLE2_TAG = ClassManifestFactory
            .fromClass((Class<Tuple2<Long, Row>>) (Class<?>) Tuple2.class);
    public static final ClassTag<Tuple2<Long, IndexedRow>> INDEXEDROW_TUPLE2_TAG = ClassManifestFactory
            .fromClass((Class<Tuple2<Long, IndexedRow>>) (Class<?>) Tuple2.class);
    public static final ClassTag<Long> LONG_TAG = ClassManifestFactory.fromClass(Long.class);
    public static final ClassTag<Row> ROW_TAG = ClassManifestFactory.fromClass(Row.class);
    public static final ClassTag<IndexedRow> INDEXEDROW_TAG = ClassManifestFactory.fromClass(IndexedRow.class);

    /**
     * Returns the first few records after a given index from an indexed RDD. If the RDD has a RangePartitioner (any
     * sorted RDD), this will be achieved by only scanning the relevant partitions.
     * 
     * @param rdd
     *            the RDD to extract the records from.
     * @param start
     *            the minimum index (inclusive) to return
     * @param limit
     *            the maximum number of records to return
     * @return the list of records corresponding to the requested page
     */
    public static <T> List<Tuple2<Long, T>> paginateAfter(JavaPairRDD<Long, T> rdd, long start, int limit) {
        if (start == 0) {
            return rdd.take(limit);
        } else {
            return rdd.filterByRange(start, Long.MAX_VALUE).take(limit);
        }
    }

    /**
     * Returns the last few records up to a given upper bound from an indexed RDD. If the RDD has a RangePartitioner
     * (any sorted RDD), this will be achieved by only scanning the relevant partitions.
     *
     * @param rdd
     *            the RDD to extract the records from.
     * @param end
     *            the minimum index (inclusive) to return
     * @param limit
     *            the maximum number of records to return
     * @return the list of records corresponding to the requested page
     */
    public static <T> List<Tuple2<Long, T>> paginateBefore(JavaPairRDD<Long, T> rdd, long end, int limit) {
        // TODO this could be optimized (see the PLL implementation which is more efficient)
        Iterator<Tuple2<Long, T>> iterator = rdd.filterByRange(Long.MIN_VALUE, end - 1).toLocalIterator();
        Deque<Tuple2<Long, T>> buffer = new ArrayDeque<>(limit);
        while (iterator.hasNext()) {
            if (buffer.size() == limit) {
                buffer.removeFirst();
            }
            buffer.addLast(iterator.next());
        }
        return new ArrayList<>(buffer);
    }

    /**
     * This is what JavaRDD.zipWithIndex really ought to do
     * 
     * @param <T>
     * @param rdd
     * @return
     */
    public static <T> JavaPairRDD<Long, T> zipWithIndex(JavaRDD<T> rdd) {
        return new ZippedWithIndexRDD<T>(rdd).asPairRDD();
    }

    /**
     * Like JavaPairRDD.mapValues in that it preserves partitioning of the underlying RDD, but the mapping function has
     * also access to the key.
     * 
     * @param pairRDD
     *            the indexed RDD to map
     * @param function
     *            a function mapping key, value to the new value
     * @return a RDD with the same partitioning as the original one, with mapped values
     */
    public static <K, V, W> JavaPairRDD<K, W> mapKeyValuesToValues(JavaPairRDD<K, V> pairRDD, Function2<K, V, W> function) {
        PairFlatMapFunction<Iterator<Tuple2<K, V>>, K, W> mapper = mapKeyValuesToValuesInternal(function);
        return pairRDD.mapPartitionsToPair(mapper, true);
    }

    private static <K, V, W> PairFlatMapFunction<Iterator<Tuple2<K, V>>, K, W> mapKeyValuesToValuesInternal(Function2<K, V, W> function) {
        return new PairFlatMapFunction<Iterator<Tuple2<K, V>>, K, W>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Iterator<Tuple2<K, W>> call(Iterator<Tuple2<K, V>> t) throws Exception {
                return new Iterator<Tuple2<K, W>>() {

                    @Override
                    public boolean hasNext() {
                        return t.hasNext();
                    }

                    @Override
                    public Tuple2<K, W> next() {
                        Tuple2<K, V> v = t.next();
                        try {
                            return new Tuple2<K, W>(v._1, function.call(v._1, v._2));
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    }

                };
            }

        };
    }

    /**
     * Performs a partition-wise limit: returns a RDD where partitions are capped to a maximum number of items.
     * 
     * This is intended to be used as a deterministic and efficient form of "sampling". Spark's own sampling is
     * non-deterministic and does not speed up computations much because it still scans the entire RDD (it is equivalent
     * to a filter).
     * 
     * @param pairRDD
     *            the RDD to limit
     * @param limit
     *            the maximum number of elements per partition
     * @return the truncated RDD
     */
    public static <K, V> JavaPairRDD<K, V> limitPartitions(JavaPairRDD<K, V> pairRDD, long limit) {
        PairFlatMapFunction<Iterator<Tuple2<K, V>>, K, V> mapper = new PairFlatMapFunction<Iterator<Tuple2<K, V>>, K, V>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Iterator<Tuple2<K, V>> call(Iterator<Tuple2<K, V>> t) throws Exception {
                return new Iterator<Tuple2<K, V>>() {

                    long seen = 0;

                    @Override
                    public boolean hasNext() {
                        return seen < limit && t.hasNext();
                    }

                    @Override
                    public Tuple2<K, V> next() {
                        seen++;
                        return t.next();
                    }

                };
            }

        };
        return pairRDD.mapPartitionsToPair(mapper, true);
    }

    /**
     * Performs a partition-wise limit: returns a RDD where partitions are capped to a maximum number of items.
     * 
     * This is intended to be used as a deterministic and efficient form of "sampling". Spark's own sampling is
     * non-deterministic and does not speed up computations much because it still scans the entire RDD (it is equivalent
     * to a filter).
     * 
     * @param pairRDD
     *            the RDD to limit
     * @param limit
     *            the maximum number of elements per partition
     * @return the truncated RDD
     */
    public static <T> JavaRDD<T> limitPartitions(JavaRDD<T> pairRDD, long limit) {
        FlatMapFunction<Iterator<T>, T> mapper = new FlatMapFunction<Iterator<T>, T>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Iterator<T> call(Iterator<T> t) throws Exception {
                return new Iterator<T>() {

                    long seen = 0;

                    @Override
                    public boolean hasNext() {
                        return seen < limit && t.hasNext();
                    }

                    @Override
                    public T next() {
                        seen++;
                        return t.next();
                    }

                };
            }

        };
        return pairRDD.mapPartitions(mapper, true);
    }

    /**
     * Performs a limit: returns a new RDD which contains the n first elements of this RDD. The supplied RDD is assumed
     * to be keyed by indices, and the resulting RDD will be more efficient if the original RDD is partitioned by key.
     * 
     * @param <V>
     * @param pairRDD
     * @param limit
     * @return
     */
    public static <V> JavaPairRDD<Long, V> limit(JavaPairRDD<Long, V> pairRDD, long limit) {
        if (pairRDD.getNumPartitions() > 1 && pairRDD.partitioner().isPresent()) {
            Partitioner partitioner = pairRDD.partitioner().get();
            if (partitioner instanceof SortedPartitioner) {
                @SuppressWarnings("unchecked")
                SortedPartitioner<Long> sortedPartitioner = (SortedPartitioner<Long>) partitioner;

                List<Long> limits = new ArrayList<>(pairRDD.getNumPartitions());
                limits.add(limit);
                for (Long firstKey : sortedPartitioner.firstKeys()) {
                    limits.add(Math.max(0, limit - firstKey));
                }

                // use the known partition starts to limit each partition to the appropriate limit
                JavaPairRDD<Long, V> limited = JavaPairRDD.fromJavaRDD(pairRDD.mapPartitionsWithIndex(new PartitionLimiter(limits), true));
                return new PartitionedRDD<Long, V>(limited, sortedPartitioner).asPairRDD(pairRDD.kClassTag(), pairRDD.vClassTag());
            }
        }
        // fallback: less efficient, but still avoids doing a full pass on the dataset
        return limitPartitions(pairRDD, limit)
                .filter(new Function<Tuple2<Long, V>, Boolean>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Boolean call(Tuple2<Long, V> v1) throws Exception {
                        return v1._1 < limit;
                    }

                });
    }

    private static class PartitionLimiter<V> implements Function2<Integer, Iterator<Tuple2<Long, V>>, Iterator<Tuple2<Long, V>>> {

        private static final long serialVersionUID = 7276022996698290108L;

        private final List<Long> _limits;

        public PartitionLimiter(List<Long> limits) {
            _limits = limits;
        }

        @Override
        public Iterator<Tuple2<Long, V>> call(Integer partitionIndex, Iterator<Tuple2<Long, V>> t) throws Exception {
            return new Iterator<Tuple2<Long, V>>() {

                long seen = 0;

                @Override
                public boolean hasNext() {
                    return seen < _limits.get(partitionIndex) && t.hasNext();
                }

                @Override
                public Tuple2<Long, V> next() {
                    seen++;
                    return t.next();
                }

            };
        }

    }

    /**
     * Groups elements of the RDD by batches of the specified size. It does so partition-wise, so the RDD may contain
     * multiple incomplete batches.
     * 
     * @param <T>
     * @param rdd
     * @param batchSize
     * @return
     */
    public static <T> JavaRDD<List<T>> partitionWiseBatching(JavaRDD<T> rdd, int batchSize) {
        FlatMapFunction<Iterator<T>, List<T>> f = partitionWiseBatchingFlatMap(batchSize);
        // the conversion to ArrayList is necessary to ensure serializability of each element
        return rdd.mapPartitions(f).map(l -> new ArrayList<T>(l));
    }

    private static <T> FlatMapFunction<Iterator<T>, List<T>> partitionWiseBatchingFlatMap(int batchSize) {
        return new FlatMapFunction<Iterator<T>, List<T>>() {

            private static final long serialVersionUID = -1984177858467149132L;

            @Override
            public Iterator<List<T>> call(Iterator<T> t) throws Exception {
                return Iterators.partition(t, (int) batchSize);
            }

        };
    }

}
