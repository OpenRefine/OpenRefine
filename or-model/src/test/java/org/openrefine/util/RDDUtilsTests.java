
package org.openrefine.util;

import java.io.Serializable;
import java.util.List;

import org.apache.spark.api.java.JavaPairRDD;
import org.testng.Assert;
import org.testng.annotations.Test;
import scala.Tuple2;

import org.openrefine.SparkBasedTest;
import org.openrefine.model.Row;

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
        List<Tuple2<Long, Row>> rows = filtered.collect();
        Assert.assertEquals(rows.size(), 2);
        Assert.assertEquals(rows.get(0)._2.getCellValue(0), 5);
        Assert.assertEquals(rows.get(1)._2.getCellValue(1), 8);
    }
}
