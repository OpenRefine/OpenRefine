
package org.openrefine.model.local.partitioning;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;

public class CroppedPartitionerTests {

    @Test
    public void testCropLongRangePartitionerLeft() {
        LongRangePartitioner partitioner = new LongRangePartitioner(5, Arrays.asList(78L, 189L, 257L, 489L));

        Partitioner<Long> croppedLeft = CroppedPartitioner.crop(partitioner, 2, false);
        Assert.assertTrue(croppedLeft instanceof LongRangePartitioner);
        Assert.assertEquals(croppedLeft.numPartitions(), 3);
        Assert.assertEquals(((LongRangePartitioner) croppedLeft).firstKeys, Arrays.asList(257L, 489L));
    }

    @Test
    public void testCropLongRangePartitionerRight() {
        LongRangePartitioner partitioner = new LongRangePartitioner(5, Arrays.asList(78L, 189L, 257L, 489L));

        Partitioner<Long> croppedRight = CroppedPartitioner.crop(partitioner, 2, true);
        Assert.assertTrue(croppedRight instanceof LongRangePartitioner);
        Assert.assertEquals(croppedRight.numPartitions(), 3);
        Assert.assertEquals(((LongRangePartitioner) croppedRight).firstKeys, Arrays.asList(78L, 189L));
    }

    @Test
    public void testCropArbitraryPartitionerLeft() {
        Partitioner<String> partitioner = (Partitioner<String>) mock(Partitioner.class);
        when(partitioner.getPartition("foo")).thenReturn(0);
        when(partitioner.getPartition("bar")).thenReturn(1);
        when(partitioner.getPartition("baz")).thenReturn(2);
        when(partitioner.getPartition("hey")).thenReturn(3);
        when(partitioner.numPartitions()).thenReturn(5);

        Partitioner<String> croppedLeft = CroppedPartitioner.crop(partitioner, 2, false);
        Assert.assertEquals(croppedLeft.numPartitions(), 3);
        Assert.assertEquals(croppedLeft.getPartition("foo"), 0); // actually here we know that "foo" cannot even be
                                                                 // there
        // but there is no way to signal that in the Partitioner interface
        Assert.assertEquals(croppedLeft.getPartition("bar"), 0); // same
        Assert.assertEquals(croppedLeft.getPartition("baz"), 0);
        Assert.assertEquals(croppedLeft.getPartition("hey"), 1);
    }

    @Test
    public void testCropArbitraryPartitionerRight() {
        Partitioner<String> partitioner = (Partitioner<String>) mock(Partitioner.class);
        when(partitioner.getPartition("foo")).thenReturn(4);
        when(partitioner.getPartition("bar")).thenReturn(3);
        when(partitioner.getPartition("baz")).thenReturn(2);
        when(partitioner.getPartition("hey")).thenReturn(1);
        when(partitioner.numPartitions()).thenReturn(5);

        Partitioner<String> croppedLeft = CroppedPartitioner.crop(partitioner, 2, true);
        Assert.assertEquals(croppedLeft.numPartitions(), 3);
        Assert.assertEquals(croppedLeft.getPartition("foo"), 2); // actually here we know that "foo" cannot even be
                                                                 // there
        // but there is no way to signal that in the Partitioner interface
        Assert.assertEquals(croppedLeft.getPartition("bar"), 2); // same
        Assert.assertEquals(croppedLeft.getPartition("baz"), 2);
        Assert.assertEquals(croppedLeft.getPartition("hey"), 1);
    }
}
