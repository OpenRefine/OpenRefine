package org.openrefine.util;

import java.io.Serializable;
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
    public void testFilterByRange() {
        JavaPairRDD<Long, Row> rdd = rowRDD(new Serializable[][] {
            { 1, 2 },
            { 3, 4 },
            { 5, 6 },
            { 7, 8 }
        });
        JavaPairRDD<Long, Row> filtered = RDDUtils.filterByRange(rdd, 2L, 5L);
        List<Tuple2<Long,Row>> rows = filtered.collect();
        Assert.assertEquals(rows.size(), 2);
        Assert.assertEquals(rows.get(0)._2.getCellValue(0), 5);
        Assert.assertEquals(rows.get(1)._2.getCellValue(1), 8);
    }
    
    @Test
    public void testLimitPartitions() {
        List<Tuple2<Long,Row>> list = new ArrayList<>();
        for(int i = 0; i < 10; i++) {
            list.add(new Tuple2<Long,Row>((long)i, new Row(
                    Arrays.asList(new Cell(i, null) ))));
        }
        JavaPairRDD<Long,Row> rdd = context().parallelize(list, 2)
                .keyBy(t -> (Long)t._1)
                .mapValues(t -> t._2);
        
        JavaPairRDD<Long,Row> capped = RDDUtils.limitPartitions(rdd, 3);
        List<Tuple2<Long,Row>> rows = capped.collect();
        Assert.assertEquals(rows.size(), 6);
        Assert.assertEquals(rows.stream().map(t -> t._1).collect(Collectors.toList()),
                Arrays.asList(0L, 1L, 2L, 5L, 6L, 7L));
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
                       Arrays.asList(15L)
                        ));
    }
}
