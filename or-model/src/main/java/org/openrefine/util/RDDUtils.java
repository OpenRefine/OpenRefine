
package org.openrefine.util;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.rdd.OrderedRDDFunctions;
import scala.Tuple2;
import scala.math.Ordering;
import scala.reflect.ClassManifestFactory;

/**
 * Collection of utilities around Spark RDDs.
 * 
 * @author Antonin Delpeuch
 *
 */
public class RDDUtils {

    /**
     * Efficiently filters a RDD which has a RangePartitioner (any sorted RDD) by pruning partitions which cannot
     * contain keys outside the range, or falls back on regular filter if no RangePartitioner is available.
     * 
     * Workaround for https://issues.apache.org/jira/browse/SPARK-31518, which will be fixed in 3.1.0
     * 
     * @todo remove this once 3.1.0 is released
     * 
     * @param <K>
     *            type of keys
     * @param <V>
     *            type of values
     * @param rdd
     *            the RDD to filter
     * @param lower
     *            the lower bound (inclusive)
     * @param upper
     *            the upper bound (exclusive)
     * @return a RDD containing only keys within the range
     */
    public static <V> JavaPairRDD<Long, V> filterByRange(JavaPairRDD<Long, V> rdd, long lower, long upper) {
        Ordering<Long> ordering = new Ordering<Long>() {

            private static final long serialVersionUID = 1L;

            @Override
            public int compare(Long x, Long y) {
                return Long.compare(x, y);
            }
        };
        return JavaPairRDD.fromRDD(new OrderedRDDFunctions<Long, V, Tuple2<Long, V>>(
                rdd.rdd(), ordering, ClassManifestFactory.fromClass(Long.class), rdd.vClassTag(), rdd.classTag())
                .filterByRange(lower, upper),
                rdd.kClassTag(), rdd.vClassTag());
    }
}
