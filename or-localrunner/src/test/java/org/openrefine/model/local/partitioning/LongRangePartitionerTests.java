
package org.openrefine.model.local.partitioning;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;

public class LongRangePartitionerTests {

    @Test
    public void testShift() {
        LongRangePartitioner partitioner = new LongRangePartitioner(4, Arrays.asList(4L, 78L, 589L));

        LongRangePartitioner shifted = partitioner.shiftKeys(10L);
        Assert.assertEquals(shifted.numPartitions(), partitioner.numPartitions());
        Assert.assertEquals(shifted.firstKeys, Arrays.asList(14L, 88L, 599L));
    }
}
