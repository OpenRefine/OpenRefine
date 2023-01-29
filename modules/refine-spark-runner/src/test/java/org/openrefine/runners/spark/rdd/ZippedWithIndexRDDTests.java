
package org.openrefine.runners.spark.rdd;

import java.util.Arrays;
import java.util.List;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.testng.Assert;
import org.testng.annotations.Test;
import scala.Tuple2;

import org.openrefine.runners.spark.SparkBasedTest;
import org.openrefine.runners.spark.rdd.SortedRDD.SortedPartitioner;

public class ZippedWithIndexRDDTests extends SparkBasedTest {

    @Test
    public void testZipWithIndex() {
        JavaRDD<String> initial = context.parallelize(Arrays.asList("foo", "bar", "hello", "world", "today"), 3);
        List<List<String>> partitions = Arrays.asList(initial.collectPartitions(new int[] { 0, 1, 2 }));
        Assert.assertEquals(partitions, Arrays.asList(
                Arrays.asList("foo"),
                Arrays.asList("bar", "hello"),
                Arrays.asList("world", "today")));

        JavaPairRDD<Long, String> zipped = new ZippedWithIndexRDD<String>(initial).asPairRDD();

        List<Tuple2<Long, String>> result = zipped.collect();

        Assert.assertTrue(zipped.partitioner().isPresent(), "The zipped RDD should have a partitioner.");
        @SuppressWarnings("unchecked")
        SortedRDD.SortedPartitioner<Long> partitioner = (SortedPartitioner<Long>) zipped.partitioner().get();
        Assert.assertEquals(partitioner.firstKeys(), Arrays.asList(1L, 3L));
        Assert.assertEquals(result, Arrays.asList(
                new Tuple2<Long, String>(0L, "foo"),
                new Tuple2<Long, String>(1L, "bar"),
                new Tuple2<Long, String>(2L, "hello"),
                new Tuple2<Long, String>(3L, "world"),
                new Tuple2<Long, String>(4L, "today")));
        Assert.assertEquals(zipped.lookup(0L), Arrays.asList("foo"));
        Assert.assertEquals(zipped.lookup(4L), Arrays.asList("today"));
    }
}
