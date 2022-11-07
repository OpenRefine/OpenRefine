
package org.openrefine.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.openrefine.SparkBasedTest;
import org.openrefine.model.Cell;
import org.openrefine.model.Row;
import org.testng.Assert;
import org.testng.annotations.Test;

import scala.Tuple2;

public class RDDUtilsTests extends SparkBasedTest {

    @Test
    public void testLimitPartitionsPair() {
        List<Tuple2<Long, Row>> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(new Tuple2<Long, Row>((long) i, new Row(
                    Arrays.asList(new Cell(i, null)))));
        }
        JavaPairRDD<Long, Row> rdd = context().parallelize(list, 2)
                .keyBy(t -> (Long) t._1)
                .mapValues(t -> t._2);
        Assert.assertFalse(rdd.partitioner().isPresent());

        JavaPairRDD<Long, Row> capped = RDDUtils.limitPartitions(rdd, 3);
        List<Tuple2<Long, Row>> rows = capped.collect();
        Assert.assertEquals(rows.size(), 6);
        Assert.assertEquals(rows.stream().map(t -> t._1).collect(Collectors.toList()),
                Arrays.asList(0L, 1L, 2L, 5L, 6L, 7L));
        Assert.assertFalse(rdd.partitioner().isPresent());
    }

    @Test
    public void testLimitPartitionsPairPreservesPartitioning() {
        List<Row> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(new Row(
                    Arrays.asList(new Cell(i, null))));
        }

        JavaPairRDD<Long, Row> rdd = RDDUtils.zipWithIndex(context().parallelize(list, 2));
        Assert.assertTrue(rdd.partitioner().isPresent());

        JavaPairRDD<Long, Row> capped = RDDUtils.limitPartitions(rdd, 3);
        Assert.assertTrue(capped.partitioner().isPresent());
        Assert.assertEquals(capped.keys().collect(),
                Arrays.asList(0L, 1L, 2L, 5L, 6L, 7L));
    }

    @Test
    public void testLimitPartitions() {
        List<Long> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add((long) i);
        }
        JavaRDD<Long> rdd = context().parallelize(list, 2);

        JavaRDD<Long> capped = RDDUtils.limitPartitions(rdd, 3);
        List<Long> rows = capped.collect();
        Assert.assertEquals(rows.size(), 6);
        Assert.assertEquals(rows, Arrays.asList(0L, 1L, 2L, 5L, 6L, 7L));
    }

    @Test
    public void testPartitionWiseBatching() {
        List<Long> list = LongStream.range(0L, 16L).boxed().collect(Collectors.toList());
        JavaRDD<Long> rdd = context().parallelize(list, 4);

        JavaRDD<List<Long>> batchedRDD = RDDUtils.partitionWiseBatching(rdd, 3);

        Assert.assertEquals(batchedRDD.collect(),
                Arrays.asList(
                        Arrays.asList(0L, 1L, 2L),
                        Arrays.asList(3L),
                        Arrays.asList(4L, 5L, 6L),
                        Arrays.asList(7L),
                        Arrays.asList(8L, 9L, 10L),
                        Arrays.asList(11L),
                        Arrays.asList(12L, 13L, 14L),
                        Arrays.asList(15L)));
    }

    @Test
    public void testLimitNoPartitioner() {
        List<Tuple2<Long, Long>> list = LongStream.range(0L, 16L).boxed().map(l -> new Tuple2<Long, Long>(l, l))
                .collect(Collectors.toList());
        JavaRDD<Tuple2<Long, Long>> rdd = context().parallelize(list, 4);

        JavaPairRDD<Long, Long> pairs = JavaPairRDD.fromJavaRDD(rdd);
        Assert.assertFalse(pairs.partitioner().isPresent());

        JavaPairRDD<Long, Long> limited = RDDUtils.limit(pairs, 6);

        Assert.assertEquals(limited.keys().collect(), LongStream.range(0L, 6L).boxed().collect(Collectors.toList()));
    }

    @Test
    public void testLimitPartitioner() {
        List<Long> list = LongStream.range(0L, 16L).boxed().collect(Collectors.toList());
        JavaRDD<Long> rdd = context().parallelize(list, 4);

        JavaPairRDD<Long, Long> pairs = RDDUtils.zipWithIndex(rdd);
        Assert.assertTrue(pairs.partitioner().isPresent());
        Assert.assertTrue(rdd.getNumPartitions() > 1);

        JavaPairRDD<Long, Long> limited = RDDUtils.limit(pairs, 6);

        Assert.assertEquals(limited.keys().collect(), LongStream.range(0L, 6L).boxed().collect(Collectors.toList()));
    }

    @Test
    public void testPaginateBefore() {
        List<Long> list = LongStream.range(0L, 16L).boxed().collect(Collectors.toList());
        JavaRDD<Long> rdd = context().parallelize(list, 4);
        JavaPairRDD<Long, Long> pairs = RDDUtils.zipWithIndex(rdd);
        List<Tuple2<Long, Long>> pairsList = pairs.collect();

        Assert.assertEquals(RDDUtils.paginateBefore(pairs, 7L, 3), pairsList.subList(4, 7));
        Assert.assertEquals(RDDUtils.paginateBefore(pairs, 3L, 8), pairsList.subList(0, 3));
        Assert.assertEquals(RDDUtils.paginateBefore(pairs, 20L, 4), pairsList.subList(12, 16));
    }
}
