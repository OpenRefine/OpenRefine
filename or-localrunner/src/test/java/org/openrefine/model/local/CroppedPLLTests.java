
package org.openrefine.model.local;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;

public class CroppedPLLTests extends PLLTestsBase {

    @Test
    public void testDropInFirstPartition() {
        PLL<Integer> pll = parallelize(4, Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));
        Assert.assertEquals(pll.getPartitionSizes(), Arrays.asList(3L, 3L, 3L, 3L));

        PLL<Integer> dropped = pll.dropFirstElements(1L);
        Assert.assertEquals(dropped.numPartitions(), 4);
        Assert.assertEquals(dropped.collect(), Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));
    }

    @Test
    public void testDropInMultiplePartitionsAtTheStart() {
        PLL<Integer> pll = parallelize(4, Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));
        Assert.assertEquals(pll.getPartitionSizes(), Arrays.asList(3L, 3L, 3L, 3L));

        PLL<Integer> dropped = pll.dropFirstElements(4L);
        Assert.assertEquals(dropped.numPartitions(), 3);
        Assert.assertEquals(dropped.collect(), Arrays.asList(4, 5, 6, 7, 8, 9, 10, 11));
    }

    @Test
    public void testDropAllFromStart() {
        PLL<Integer> pll = parallelize(4, Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));

        PLL<Integer> dropped = pll.dropFirstElements(42L);
        Assert.assertEquals(dropped.numPartitions(), 0);
        Assert.assertTrue(dropped.isEmpty());
    }

    @Test
    public void testDropInLastPartition() {
        PLL<Integer> pll = parallelize(4, Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));
        Assert.assertEquals(pll.getPartitionSizes(), Arrays.asList(3L, 3L, 3L, 3L));

        PLL<Integer> dropped = pll.dropLastElements(1L);
        Assert.assertEquals(dropped.numPartitions(), 4);
        Assert.assertEquals(dropped.collect(), Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    @Test
    public void testDropInMultiplePartitionsAtTheEnd() {
        PLL<Integer> pll = parallelize(4, Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));
        Assert.assertEquals(pll.getPartitionSizes(), Arrays.asList(3L, 3L, 3L, 3L));

        PLL<Integer> dropped = pll.dropLastElements(4L);
        Assert.assertEquals(dropped.numPartitions(), 3);
        Assert.assertEquals(dropped.collect(), Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
    }

    @Test
    public void testDropAllFromEnd() {
        PLL<Integer> pll = parallelize(4, Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));

        PLL<Integer> dropped = pll.dropLastElements(42L);
        Assert.assertEquals(dropped.numPartitions(), 0);
        Assert.assertTrue(dropped.isEmpty());
    }

}
