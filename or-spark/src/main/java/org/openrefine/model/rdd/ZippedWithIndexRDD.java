
package org.openrefine.model.rdd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.spark.Partition;
import org.apache.spark.TaskContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.rdd.RDD;
import scala.Function2;
import scala.Serializable;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.JavaConverters;
import scala.collection.Seq;
import scala.reflect.ClassManifestFactory;
import scala.reflect.ClassManifestFactory$;
import scala.reflect.ClassTag;

/**
 * A RDD which adds an index to its elements. This index is used as key, not as value, unlike Spark's
 * {@link RDD.zipWithIndex()}.
 * 
 * This RDD is suitable for partitioning with {@link SortedRDD}.
 * 
 * @author Antonin Delpeuch
 *
 * @param <T>
 */
public class ZippedWithIndexRDD<T> extends RDD<Tuple2<Long, T>> {

    private static final long serialVersionUID = -7865339028373715646L;

    private static final ClassTag<Long> LONG_TAG = ClassManifestFactory$.MODULE$.fromClass(Long.class);

    private final List<Long> partitionLengths;
    private final ClassTag<T> valueClassTag;

    public ZippedWithIndexRDD(RDD<T> parent, ClassTag<Tuple2<Long, T>> tupleClassTag) {
        super(parent, tupleClassTag);
        partitionLengths = fetchLengths(parent);
        valueClassTag = parent.elementClassTag();
    }

    @SuppressWarnings("unchecked")
    public ZippedWithIndexRDD(JavaRDD<T> parent) {
        this(parent.rdd(), ClassManifestFactory.fromClass((Class<Tuple2<Long, T>>) (Class<?>) Tuple2.class));
    }

    public JavaPairRDD<Long, T> asPairRDD() {
        return new JavaPairRDD<Long, T>(this, LONG_TAG, valueClassTag);
    }

    private static <T> List<Long> fetchLengths(RDD<T> parent) {
        int numPartitions = parent.getNumPartitions();
        List<Long> partitionLengths = Collections.emptyList();

        if (parent.getNumPartitions() > 1) {
            List<Object> partitionIds = new ArrayList<>(numPartitions - 1);
            for (int i = 0; i != numPartitions - 1; i++) {
                partitionIds.add(i);
            }
            Seq<Object> partitionIdObjs = JavaConverters.collectionAsScalaIterable(partitionIds).toSeq();
            // casting directly to K[] can fail
            Object[] objects = (Object[]) parent.context().runJob(parent, new CountPartitionElements<T>(), partitionIdObjs, LONG_TAG);

            partitionLengths = new ArrayList<>(objects.length);
            for (int i = 0; i != objects.length; i++) {
                partitionLengths.add((long) objects[i]);
            }
        }
        return partitionLengths;
    }

    @Override
    public Iterator<Tuple2<Long, T>> compute(Partition arg0, TaskContext context) {
        ZippedWithIndexRDDPartition partition = (ZippedWithIndexRDDPartition) arg0;
        Iterator<T> origIter = this.firstParent(valueClassTag).iterator(partition.prev, context);
        return zipIteratorWithIndex(origIter, partition.startIndex);
    }

    @Override
    public Partition[] getPartitions() {
        Partition[] origPartitions = this.firstParent(elementClassTag()).getPartitions();
        Partition[] newPartitions = new Partition[origPartitions.length];

        long offset = 0L;
        for (int i = 0; i != origPartitions.length; i++) {
            newPartitions[i] = new ZippedWithIndexRDDPartition(origPartitions[i], offset);
            if (i > 0 && i < origPartitions.length - 1) {
                offset += partitionLengths.get(i - 1);
            }
        }
        return newPartitions;
    }

    private static <T> Iterator<Tuple2<Long, T>> zipIteratorWithIndex(Iterator<T> iterator, long startIndex) {
        return new Iterator<Tuple2<Long, T>>() {

            long currentIndex = startIndex - 1L;

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Tuple2<Long, T> next() {
                currentIndex += 1L;
                return new Tuple2<Long, T>(currentIndex, iterator.next());
            }

        };
    }

    protected static class ZippedWithIndexRDDPartition implements Partition {

        private static final long serialVersionUID = 8256733549359673469L;

        private final Partition prev;
        protected final long startIndex;

        ZippedWithIndexRDDPartition(Partition prev, long startIndex) {
            this.prev = prev;
            this.startIndex = startIndex;
        }

        @Override
        public int index() {
            return prev.index();
        }

    }

    /**
     * Counts the number of elements in a partition. This requires iterating over the partition.
     */
    protected static class CountPartitionElements<T> implements Function2<TaskContext, Iterator<T>, Long>, Serializable {

        private static final long serialVersionUID = 3187776505554778763L;

        @Override
        public Long apply(TaskContext v1, Iterator<T> iterator) {
            long count = 0;
            if (iterator.hasNext()) {
                iterator.next();
                count++;
            }
            return count;
        }
    }

}
