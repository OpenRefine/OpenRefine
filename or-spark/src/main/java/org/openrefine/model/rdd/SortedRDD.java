
package org.openrefine.model.rdd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.apache.spark.Partitioner;
import org.apache.spark.TaskContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.rdd.RDD;
import scala.Function2;
import scala.Serializable;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.JavaConverters;
import scala.collection.Seq;
import scala.reflect.ClassTag;

/**
 * A RDD of pairs which is aware of the ordering of its keys. This can be used to add a partitioner to an existing
 * JavaPairRDD whose keys are already known to be sorted, without requiring any actual sorting and shuffle.
 * 
 * This makes it possible to optimize certain operations, such as retrieving records by keys with the lookup method.
 * 
 * This class assumes that keys are unique.
 * 
 * Workaround for https://issues.apache.org/jira/browse/SPARK-1061
 * 
 * @author Antonin Delpeuch
 *
 */
public class SortedRDD<K extends Comparable<K>, V> extends PartitionedRDD<K, V> {

    private static final long serialVersionUID = -5438240253771415945L;

    protected final ClassTag<K> keyClassTag;

    public SortedRDD(JavaPairRDD<K, V> pairRDD) {
        this(pairRDD.rdd(), pairRDD.classTag(), pairRDD.kClassTag());
    }

    public SortedRDD(RDD<Tuple2<K, V>> parent, ClassTag<Tuple2<K, V>> tupleClassTag, ClassTag<K> keyClassTag) {
        super(parent, createPartitioner(parent, keyClassTag), tupleClassTag);
        this.keyClassTag = keyClassTag;
    }

    private static <K extends Comparable<K>, V> Partitioner createPartitioner(RDD<Tuple2<K, V>> parent, ClassTag<K> keyClassTag) {
        int numPartitions = parent.getNumPartitions();
        List<K> firstRowIds = null;

        if (parent.getNumPartitions() > 1) {
            List<Object> partitionIds = new ArrayList<>(numPartitions - 1);
            for (int i = 1; i != numPartitions; i++) {
                partitionIds.add(i);
            }
            Seq<Object> partitionIdObjs = JavaConverters.collectionAsScalaIterable(partitionIds).toSeq();
            // casting directly to K[] can fail
            Object[] objects = (Object[]) parent.context().runJob(parent, new ExtractFirstRowId<K, V>(), partitionIdObjs, keyClassTag);

            firstRowIds = new ArrayList<>(objects.length);
            for (int i = 0; i != objects.length; i++) {
                firstRowIds.add((K) objects[i]);
            }
        }
        return new SortedPartitioner<K>(numPartitions, firstRowIds);
    }

    public JavaPairRDD<K, V> asPairRDD(ClassTag<V> valueClassTag) {
        return asPairRDD(keyClassTag, valueClassTag);
    }

    public static <K extends Comparable<K>, V> JavaPairRDD<K, V> assumeSorted(JavaPairRDD<K, V> rdd) {
        if (rdd.partitioner().isPresent()) {
            return rdd;
        } else {
            return new SortedRDD<K, V>(rdd).asPairRDD(rdd.vClassTag());
        }
    }

    public static class SortedPartitioner<T extends Comparable<T>> extends Partitioner {

        private static final long serialVersionUID = -4094768875640860732L;

        private final int numPartitions;
        private final List<T> firstKeys;

        public SortedPartitioner(int numPartitions, List<T> firstRowIds) {
            this.numPartitions = numPartitions;
            this.firstKeys = firstRowIds;
            if (numPartitions > 1) {
                Validate.isTrue(firstKeys.size() == numPartitions - 1);
            }
        }

        @Override
        public int getPartition(Object key) {
            if (numPartitions <= 1) {
                return 0;
            }
            @SuppressWarnings("unchecked")
            T comparableKey = (T) key;

            // TODO use binary search if numPartitions > 128, as Spark does
            int idx = 0;
            int lastNonEmpty = 0;
            while (idx + 1 < numPartitions && (firstKeys.get(idx) == null || firstKeys.get(idx).compareTo(comparableKey) <= 0)) {
                if (firstKeys.get(idx) != null) {
                    lastNonEmpty = idx + 1;
                }
                idx++;
            }
            return lastNonEmpty;
        }

        @Override
        public int numPartitions() {
            return numPartitions;
        }

        public List<T> firstKeys() {
            return firstKeys == null ? Collections.emptyList() : firstKeys;
        }

    }

    /**
     * Extracts the first key of a partition, or returns null if the partition is empty.
     */
    protected static class ExtractFirstRowId<K, V> implements Function2<TaskContext, Iterator<Tuple2<K, V>>, K>, Serializable {

        private static final long serialVersionUID = 3187776505554778763L;

        @Override
        public K apply(TaskContext v1, Iterator<Tuple2<K, V>> iterator) {
            if (iterator.hasNext()) {
                return iterator.next()._1;
            } else {
                return null;
            }
        }
    }
}
