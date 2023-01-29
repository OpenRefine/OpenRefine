
package org.openrefine.runners.spark.rdd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.spark.Partitioner;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.testng.Assert;
import org.testng.annotations.Test;
import scala.Tuple2;
import scala.reflect.ClassManifestFactory;
import scala.reflect.ClassTag;

import org.openrefine.model.Cell;
import org.openrefine.model.Row;
import org.openrefine.runners.spark.SparkBasedTest;

public class SortedRDDTests extends SparkBasedTest {

    private static final ClassTag<Long> LONG_TAG = ClassManifestFactory.fromClass(Long.class);
    @SuppressWarnings("unchecked")
    private static final ClassTag<Tuple2<Long, String>> tupleTag = ClassManifestFactory
            .fromClass((Class<Tuple2<Long, String>>) (Class<?>) Tuple2.class);

    @Test
    public void testPartitioner() {
        Partitioner partitioner = new SortedRDD.SortedPartitioner<Integer>(5, Arrays.asList(3, null, 8, 19));
        Assert.assertEquals(partitioner.getPartition(-1), 0);
        Assert.assertEquals(partitioner.getPartition(1), 0);
        Assert.assertEquals(partitioner.getPartition(3), 1);
        Assert.assertEquals(partitioner.getPartition(5), 1);
        Assert.assertEquals(partitioner.getPartition(5), 1);
        Assert.assertEquals(partitioner.getPartition(9), 3);
        Assert.assertEquals(partitioner.getPartition(9), 3);
        Assert.assertEquals(partitioner.getPartition(24), 4);
        Assert.assertEquals(partitioner.numPartitions(), 5);
    }

    @Test
    public void testEmptyPartitioner() {
        Partitioner partitioner = new SortedRDD.SortedPartitioner<Integer>(1, null);
        Assert.assertEquals(partitioner.getPartition(42), 0);
        Assert.assertEquals(partitioner.numPartitions(), 1);
    }

    @Test
    public void testSortedRDD() {
        List<Tuple2<Long, String>> data = new ArrayList<>();
        data.add(new Tuple2<Long, String>(0L, "once"));
        data.add(new Tuple2<Long, String>(1L, "upon"));
        data.add(new Tuple2<Long, String>(2L, "a"));
        data.add(new Tuple2<Long, String>(3L, "time"));
        data.add(new Tuple2<Long, String>(4L, "in"));
        data.add(new Tuple2<Long, String>(5L, "the"));
        data.add(new Tuple2<Long, String>(6L, "west"));
        JavaRDD<Tuple2<Long, String>> rdd = context().parallelize(data, 3);

        // We parallelized a dataset which is already sorted by key, but Spark is anaware of it.
        Assert.assertFalse(rdd.partitioner().isPresent());

        // We wrap it into a SortedRDD
        SortedRDD<Long, String> sortedRDD = new SortedRDD<Long, String>(rdd.rdd(), tupleTag, LONG_TAG);

        // Now it has got a partitioner
        Assert.assertTrue(sortedRDD.partitioner().get() instanceof SortedRDD.SortedPartitioner<?>);

        // Looking up rows still works
        JavaPairRDD<Long, String> pairRDD = sortedRDD.asPairRDD(ClassManifestFactory.fromClass(String.class));
        Assert.assertEquals(pairRDD.lookup(0L), Collections.singletonList("once"));
        Assert.assertEquals(pairRDD.lookup(1L), Collections.singletonList("upon"));
        Assert.assertEquals(pairRDD.lookup(2L), Collections.singletonList("a"));
        Assert.assertEquals(pairRDD.lookup(3L), Collections.singletonList("time"));
        Assert.assertEquals(pairRDD.lookup(4L), Collections.singletonList("in"));
        Assert.assertEquals(pairRDD.lookup(5L), Collections.singletonList("the"));
        Assert.assertEquals(pairRDD.lookup(6L), Collections.singletonList("west"));
    }

    @Test
    public void testEmptyRDD() {
        List<Tuple2<Long, String>> data = new ArrayList<>();
        JavaRDD<Tuple2<Long, String>> rdd = context().parallelize(data, 1);
        SortedRDD<Long, String> sortedRDD = new SortedRDD<Long, String>(rdd.rdd(), tupleTag, LONG_TAG);
        Assert.assertTrue(sortedRDD.partitioner().isDefined());
    }

    @Test
    public void testAssumeSorted() {
        List<Tuple2<Long, Row>> data = new ArrayList<>();
        data.add(new Tuple2<Long, Row>(0L, new Row(Collections.singletonList(new Cell("hello", null)))));
        data.add(new Tuple2<Long, Row>(1L, new Row(Collections.singletonList(new Cell("world", null)))));
        JavaRDD<Tuple2<Long, Row>> rdd = context().parallelize(data, 2);
        JavaPairRDD<Long, Row> pairRDD = rdd.keyBy(f -> f._1).mapValues(f -> f._2);

        JavaPairRDD<Long, Row> sorted = SortedRDD.assumeSorted(pairRDD);
        Assert.assertTrue(sorted.partitioner().isPresent());

        // sorting a RDD twice does nothing
        JavaPairRDD<Long, Row> sortedAgain = SortedRDD.assumeSorted(sorted);
        Assert.assertEquals(sorted, sortedAgain);
    }

}
