
package org.openrefine.model.rdd;

import org.apache.spark.Partition;
import org.apache.spark.Partitioner;
import org.apache.spark.TaskContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.rdd.RDD;
import scala.Option;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.reflect.ClassTag;

/**
 * A RDD obtained by adding a specified partitioner on an existing RDD. This does not repartition the RDD using the
 * partitioner: it just assumes that the RDD is already appropriately partitioned.
 * <p>
 * Workaround for <a href="https://issues.apache.org/jira/browse/SPARK-1061">SPARK-1061</a>
 * <p>
 * TODO add the ability to verify that the RDD is indeed correctly partitioned when iterating on it.
 *
 * @author Antonin Delpeuch
 */
public class PartitionedRDD<K extends Comparable<K>, V> extends RDD<Tuple2<K, V>> {

    private static final long serialVersionUID = 5082128736394708076L;

    protected final Partitioner partitioner;

    /**
     * Creates a new RDD depending on the provided RDD, and applying the given partitioner.
     * 
     * @param pairRDD
     *            the RDD assumed to be sorted using the partitioner
     * @param partitioner
     */
    public PartitionedRDD(JavaPairRDD<K, V> pairRDD, Partitioner partitioner) {
        this(pairRDD.rdd(), partitioner, pairRDD.classTag());
    }

    /**
     * Creates a new RDD depending on the provided RDD, and applying the given partitioner.
     * 
     * @param parent
     *            the RDD assumed to be sorted using the partitioner
     * @param partitioner
     * @param tupleClassTag
     */
    public PartitionedRDD(RDD<Tuple2<K, V>> parent, Partitioner partitioner, ClassTag<Tuple2<K, V>> tupleClassTag) {
        super(parent, tupleClassTag);
        this.partitioner = partitioner;
    }

    /**
     * Convert to Java API
     */
    public JavaPairRDD<K, V> asPairRDD(ClassTag<K> keyClassTag, ClassTag<V> valueClassTag) {
        return new JavaPairRDD<K, V>(this, keyClassTag, valueClassTag);
    }

    @Override
    public Option<Partitioner> partitioner() {
        return Option.apply(partitioner);
    }

    @Override
    public Iterator<Tuple2<K, V>> compute(Partition partition, TaskContext context) {
        return firstParent(elementClassTag()).compute(partition, context);
    }

    @Override
    public Partition[] getPartitions() {
        return firstParent(elementClassTag()).getPartitions();
    }
}
