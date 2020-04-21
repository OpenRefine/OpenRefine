
package org.openrefine.model;

import java.util.List;

import org.apache.spark.api.java.JavaPairRDD;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import org.openrefine.SparkBasedTest;

public class RecordRDDTests extends SparkBasedTest {

    @DataProvider(name = "partitionNumbers")
    public Object[][] getPartitionNumbers() {
        return new Object[][] { { 1 }, { 2 }, { 4 } };
    }

    @Test(dataProvider = "partitionNumbers")
    public void testSplitRecordsOverPartitions(int numPartitions) {
        JavaPairRDD<Long, Row> rdd = rowRDD(new Cell[][] {
                new Cell[] { new Cell("a", null), new Cell("b", null) },
                new Cell[] { new Cell("", null), new Cell("c", null) },
                new Cell[] { null, new Cell("d", null) },
                new Cell[] { new Cell("e", null), new Cell("f", null) },
                new Cell[] { null, new Cell("g", null) },
                new Cell[] { new Cell("", null), new Cell("h", null) },
                new Cell[] { null, new Cell("i", null) },
                new Cell[] { new Cell("j", null), new Cell("k", null) },
        }, numPartitions);

        RecordRDD recordRDD = new RecordRDD(rdd, 0);

        List<Record> records = recordRDD.toJavaRDD().collect();
        Record record0 = records.get(0);
        Assert.assertEquals(record0.getStartRowId(), 0);
        Assert.assertEquals(record0.getRows().size(), 3);
        Record record1 = records.get(1);
        Assert.assertEquals(record1.getStartRowId(), 3);
        Assert.assertEquals(record1.getRows().size(), 4);
        Record record2 = records.get(2);
        Assert.assertEquals(record2.getStartRowId(), 7);
        Assert.assertEquals(record2.getRows().size(), 1);
        Assert.assertEquals(records.size(), 3);
    }

    @Test(dataProvider = "partitionNumbers")
    public void testNoRecordKey(int numPartitions) {
        JavaPairRDD<Long, Row> rdd = rowRDD(new Cell[][] {
                new Cell[] { new Cell("", null), new Cell("b", null) },
                new Cell[] { new Cell("", null), new Cell("c", null) },
                new Cell[] { null, new Cell("d", null) },
                new Cell[] { null, new Cell("f", null) },
                new Cell[] { null, new Cell("g", null) },
                new Cell[] { new Cell("", null), new Cell("h", null) },
                new Cell[] { null, new Cell("i", null) },
                new Cell[] { new Cell("", null), new Cell("k", null) },
        }, numPartitions);

        RecordRDD recordRDD = new RecordRDD(rdd, 0);

        List<Record> records = recordRDD.toJavaRDD().collect();
        Record record0 = records.get(0);
        Assert.assertEquals(record0.getStartRowId(), 0);
        Assert.assertEquals(record0.getRows().size(), 8);
        Assert.assertEquals(records.size(), 1);
    }

    @Test(dataProvider = "partitionNumbers")
    public void testCustomRecordKey(int numPartitions) {
        JavaPairRDD<Long, Row> rdd = rowRDD(new Cell[][] {
                new Cell[] { new Cell("a", null), new Cell("b", null) },
                new Cell[] { new Cell("", null), new Cell("c", null) },
                new Cell[] { null, new Cell("d", null) },
                new Cell[] { new Cell("e", null), new Cell("f", null) },
                new Cell[] { null, new Cell("g", null) },
                new Cell[] { new Cell("", null), new Cell("h", null) },
                new Cell[] { null, new Cell("i", null) },
                new Cell[] { new Cell("j", null), new Cell("k", null) },
        }, numPartitions);

        RecordRDD recordRDD = new RecordRDD(rdd, 1);

        List<Record> records = recordRDD.toJavaRDD().collect();
        Record record0 = records.get(0);
        Assert.assertEquals(record0.getStartRowId(), 0);
        Assert.assertEquals(record0.getRows().size(), 1);
        Record record1 = records.get(1);
        Assert.assertEquals(record1.getStartRowId(), 1);
        Assert.assertEquals(record1.getRows().size(), 1);
        Record record2 = records.get(2);
        Assert.assertEquals(record2.getStartRowId(), 2);
        Assert.assertEquals(record2.getRows().size(), 1);
        Assert.assertEquals(records.size(), 8);
    }

}
