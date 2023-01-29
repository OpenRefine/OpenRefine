
package org.openrefine.runners.local.pll;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.runners.local.pll.partitioning.Partitioner;

public class IndexedPLLTests extends PLLTestsBase {

    @Test
    public void testPartitioner() {
        PLL<Integer> pll = parallelize(4, Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));

        PairPLL<Long, Integer> indexed = IndexedPLL.index(pll);

        Partitioner<Long> partitioner = indexed.getPartitioner().get();

        Assert.assertEquals(partitioner.getPartition(0L), 0);
        Assert.assertEquals(partitioner.getPartition(1L), 0);
        Assert.assertEquals(partitioner.getPartition(2L), 1);
        Assert.assertEquals(partitioner.getPartition(3L), 1);
        Assert.assertEquals(partitioner.getPartition(4L), 2);
        Assert.assertEquals(partitioner.getPartition(5L), 2);
        Assert.assertEquals(partitioner.getPartition(6L), 3);
        Assert.assertEquals(partitioner.getPartition(7L), 3);
    }
}
