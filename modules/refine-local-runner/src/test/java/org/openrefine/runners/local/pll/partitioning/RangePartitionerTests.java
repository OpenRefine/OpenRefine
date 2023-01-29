
package org.openrefine.runners.local.pll.partitioning;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RangePartitionerTests {

    @Test
    public void testPartitioner() {
        Partitioner<Long> partitioner = new LongRangePartitioner(5, Arrays.asList(3L, null, 8L, 19L));
        Assert.assertEquals(partitioner.getPartition(-1L), 0);
        Assert.assertEquals(partitioner.getPartition(1L), 0);
        Assert.assertEquals(partitioner.getPartition(3L), 1);
        Assert.assertEquals(partitioner.getPartition(5L), 1);
        Assert.assertEquals(partitioner.getPartition(5L), 1);
        Assert.assertEquals(partitioner.getPartition(9L), 3);
        Assert.assertEquals(partitioner.getPartition(9L), 3);
        Assert.assertEquals(partitioner.getPartition(24L), 4);
        Assert.assertEquals(partitioner.numPartitions(), 5);
    }

}
