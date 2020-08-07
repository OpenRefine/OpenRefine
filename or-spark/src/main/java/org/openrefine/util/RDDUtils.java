package org.openrefine.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.rdd.OrderedRDDFunctions;
import org.openrefine.model.Row;
import org.openrefine.model.rdd.SortedRDD;
import org.openrefine.model.rdd.ZippedWithIndexRDD;

import com.google.common.collect.Iterators;

import scala.Tuple2;
import scala.math.Ordering;
import scala.reflect.ClassManifestFactory;
import scala.reflect.ClassTag;

/**
 * Collection of utilities around Spark RDDs.
 * 
 * @author Antonin Delpeuch
 *
 */
public class RDDUtils {
    
    @SuppressWarnings("unchecked")
    public static final ClassTag<Tuple2<Long,Row>> ROW_TUPLE2_TAG =
            ClassManifestFactory.fromClass((Class<Tuple2<Long,Row>>) (Class<?>)Tuple2.class);
    public static final ClassTag<Long> LONG_TAG = ClassManifestFactory.fromClass(Long.class);
    public static final ClassTag<Row> ROW_TAG = ClassManifestFactory.fromClass(Row.class);
    
    /**
     * Returns the first few records after a given index from an indexed RDD.
     * If the RDD has a RangePartitioner (any sorted RDD), this will be achieved by 
     * only scanning the relevant partitions.
     * 
     * @param rdd the RDD to extract the records from.
     * @param start the minimum index (inclusive) to return
     * @param limit the maximum number of records to return
     * @return the list of records corresponding to the requested page
     */
    public static <T> List<Tuple2<Long, T>> paginate(JavaPairRDD<Long,T> rdd, long start, int limit) {
        if (start == 0) {
            return rdd.take(limit);
        } else {
            return filterByRange(rdd, start, Long.MAX_VALUE).take(limit);
        }
    }
    
    /**
     * This is what JavaRDD.zipWithIndex really ought to do
     * @param <T>
     * @param rdd
     * @return
     */
    public static <T> JavaPairRDD<Long, T> zipWithIndex(JavaRDD<T> rdd) {
        return SortedRDD.assumeSorted(new ZippedWithIndexRDD<T>(rdd).asPairRDD());
    }
    
    /**
     * Efficiently filters a RDD which has a RangePartitioner (any sorted RDD)
     * by pruning partitions which cannot contain keys outside the range, or falls
     * back on regular filter if no RangePartitioner is available.
     * 
     * Workaround for https://issues.apache.org/jira/browse/SPARK-31518, which will 
     * be fixed in 3.1.0
     * 
     * @todo remove this once 3.1.0 is released
     * 
     * @param <K> type of keys
     * @param <V> type of values
     * @param rdd the RDD to filter
     * @param lower the lower bound (inclusive)
     * @param upper the upper bound (exclusive)
     * @return a RDD containing only keys within the range
     */
    public static <V> JavaPairRDD<Long,V> filterByRange(JavaPairRDD<Long,V> rdd, long lower, long upper) {
        Ordering<Long> ordering = new Ordering<Long>() {
            private static final long serialVersionUID = 1L;

            @Override
            public int compare(Long x, Long y) {
                return Long.compare(x, y);
            }};
        return JavaPairRDD.fromRDD(new OrderedRDDFunctions<Long,V,Tuple2<Long,V>>(
                rdd.rdd(), ordering, ClassManifestFactory.fromClass(Long.class), rdd.vClassTag(), rdd.classTag())
                .filterByRange(lower, upper),
                rdd.kClassTag(), rdd.vClassTag());
    }
    
    /**
     * Like JavaPairRDD.mapValues in that it preserves partitioning of the underlying RDD,
     * but the mapping function has also access to the key.
     * 
     * @param pairRDD
     *    the indexed RDD to map
     * @param function
     *    a function mapping key, value to the new value
     * @return
     *    a RDD with the same partitioning as the original one, with mapped values
     */
    public static <K,V,W> JavaPairRDD<K, W> mapKeyValuesToValues(JavaPairRDD<K, V> pairRDD, Function2<K, V, W> function) {
        PairFlatMapFunction<Iterator<Tuple2<K,V>>, K, W> mapper = mapKeyValuesToValuesInternal(function);
        return pairRDD.mapPartitionsToPair(mapper, true);
    }
    
    private static <K,V,W> PairFlatMapFunction<Iterator<Tuple2<K,V>>, K, W> mapKeyValuesToValuesInternal(Function2<K, V, W> function) {
        return new PairFlatMapFunction<Iterator<Tuple2<K,V>>, K, W>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Iterator<Tuple2<K, W>> call(Iterator<Tuple2<K, V>> t) throws Exception {
                return new Iterator<Tuple2<K,W>> () {

                    @Override
                    public boolean hasNext() {
                        return t.hasNext();
                    }

                    @Override
                    public Tuple2<K, W> next() {
                        Tuple2<K, V> v = t.next();
                        try {
                            return new Tuple2<K,W>(v._1, function.call(v._1, v._2));
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    }
                    
                };
            }
            
        };
    }
    
    /**
     * Performs a partition-wise limit: returns a RDD where partitions are capped to a maximum
     * number of items.
     * 
     * This is intended to be used as a deterministic and efficient form of "sampling".
     * Spark's own sampling is non-deterministic and does not speed up computations much because
     * it still scans the entire RDD (it is equivalent to a filter).
     * 
     * @param pairRDD
     *      the RDD to limit
     * @param limit
     *      the maximum number of elements per partition
     * @return
     *      the truncated RDD
     */
    public static <K,V> JavaPairRDD<K, V> limitPartitions(JavaPairRDD<K, V> pairRDD, long limit) {
        PairFlatMapFunction<Iterator<Tuple2<K,V>>, K, V> mapper = new PairFlatMapFunction<Iterator<Tuple2<K,V>>, K, V>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Iterator<Tuple2<K, V>> call(Iterator<Tuple2<K, V>> t) throws Exception {
                return new Iterator<Tuple2<K,V>> () {
                    
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
     * Groups elements of the RDD by batches of the specified size.
     * It does so partition-wise, so the RDD may contain multiple incomplete
     * batches.
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
                return Iterators.partition(t, (int)batchSize);
            }
            
        };
    }
    
}
