
package org.openrefine.model.rdd;

import java.io.Serializable;
import java.util.List;

import org.apache.spark.Partitioner;
import org.apache.spark.api.java.JavaPairRDD;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import org.openrefine.SparkBasedTest;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Record;

public class RecordRDDTests extends SparkBasedTest {

    @DataProvider(name = "partitionNumbers")
    public Object[][] getPartitionNumbers() {
        return new Object[][] { { 1 }, { 2 }, { 4 } };
    }

    @Test(dataProvider = "partitionNumbers")
    public void testSplitRecordsOverPartitions(int numPartitions) {
        JavaPairRDD<Long, IndexedRow> rdd = indexedRowRDD(new Serializable[][] {
                { "a", "b" },
                { "", "c" },
                { null, "d" },
                { "e", "f" },
                { null, "g" },
                { "", "h" },
                { null, "i" },
                { "j", "k" },
        }, numPartitions);

        RecordRDD recordRDD = new RecordRDD(rdd, 0);

        List<Record> records = recordRDD.toJavaPairRDD().values().collect();
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
        JavaPairRDD<Long, IndexedRow> rdd = indexedRowRDD(new Serializable[][] {
                { "", "b" },
                { "", "c" },
                { null, "d" },
                { null, "f" },
                { null, "g" },
                { "", "h" },
                { null, "i" },
                { "", "k" },
        }, numPartitions);

        RecordRDD recordRDD = new RecordRDD(rdd, 0);

        List<Record> records = recordRDD.toJavaPairRDD().values().collect();
        Record record0 = records.get(0);
        Assert.assertEquals(record0.getStartRowId(), 0);
        Assert.assertEquals(record0.getRows().size(), 8);
        Assert.assertEquals(records.size(), 1);
    }

    @Test(dataProvider = "partitionNumbers")
    public void testAllBlank(int numPartitions) {
        JavaPairRDD<Long, IndexedRow> rdd = indexedRowRDD(new Serializable[][] {
                { "", null },
                { "", "" },
                { null, null },
                { null, "" },
        }, numPartitions);

        RecordRDD recordRDD = new RecordRDD(rdd, 0);

        List<Record> records = recordRDD.toJavaPairRDD().values().collect();
        Record record0 = records.get(0);
        Assert.assertEquals(record0.getStartRowId(), 0);
        Assert.assertEquals(record0.getRows().size(), 1);
        Record record1 = records.get(1);
        Assert.assertEquals(record1.getStartRowId(), 1);
        Assert.assertEquals(record1.getRows().size(), 1);
        Assert.assertEquals(records.size(), 4);
    }

    @Test(dataProvider = "partitionNumbers")
    public void testCustomRecordKey(int numPartitions) {
        JavaPairRDD<Long, IndexedRow> rdd = indexedRowRDD(new Serializable[][] {
                { "a", "b" },
                { "", "c" },
                { null, "d" },
                { "e", "f" },
                { null, "g" },
                { "", "h" },
                { null, "i" },
                { "j", "k" },
        }, numPartitions);

        RecordRDD recordRDD = new RecordRDD(rdd, 1);

        List<Record> records = recordRDD.toJavaPairRDD().values().collect();
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

    @Test
    public void testPartitioner() {
        JavaPairRDD<Long, IndexedRow> rdd = indexedRowRDD(new Serializable[][] {
                { "a", "b" },
                { "", "c" },
                { null, "d" },
                { "e", "f" },
                { null, "g" },
                { "", "h" },
                { null, "i" },
                { "j", "k" },
        }, 4);
        JavaPairRDD<Long, IndexedRow> sortedRDD = SortedRDD.assumeSorted(rdd);
        Partitioner partitioner = sortedRDD.partitioner().get();

        // preliminary check that the data is partitioned as we expect for this test
        Assert.assertEquals(partitioner.getPartition(0L), 0);
        Assert.assertEquals(partitioner.getPartition(1L), 0);
        Assert.assertEquals(partitioner.getPartition(2L), 1);
        Assert.assertEquals(partitioner.getPartition(3L), 1);
        Assert.assertEquals(partitioner.getPartition(4L), 2);
        Assert.assertEquals(partitioner.getPartition(5L), 2);
        Assert.assertEquals(partitioner.getPartition(6L), 3);
        Assert.assertEquals(partitioner.getPartition(7L), 3);

        RecordRDD recordRDD = new RecordRDD(sortedRDD, 0);

        Partitioner recordPartitioner = recordRDD.partitioner().get();
        // we test the record partitioner on row ids that are not record starts
        // because this partitioner should also be suitable to partition the records flattened
        // as a list of rows. This removes the need to use a SortedRDD downstream.
        Assert.assertEquals(recordPartitioner.getPartition(0L), 0);
        Assert.assertEquals(recordPartitioner.getPartition(1L), 0);
        Assert.assertEquals(recordPartitioner.getPartition(2L), 0);
        Assert.assertEquals(recordPartitioner.getPartition(3L), 1);
        Assert.assertEquals(recordPartitioner.getPartition(4L), 1);
        Assert.assertEquals(recordPartitioner.getPartition(5L), 1);
        Assert.assertEquals(recordPartitioner.getPartition(6L), 1);
        Assert.assertEquals(recordPartitioner.getPartition(7L), 3);
    }

}
