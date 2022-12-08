
package org.openrefine.model.local;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;

public class UnionPLLTests extends PLLTestsBase {

    @Test
    public void testUnion() {
        PLL<Integer> first = parallelize(2, Arrays.asList(0, 1, 2, 3));
        PLL<Integer> second = parallelize(2, Arrays.asList(10, 11, 12, 13));
        Assert.assertEquals(first.cachedPartitionSizes, Arrays.asList(2L, 2L));
        Assert.assertEquals(second.cachedPartitionSizes, Arrays.asList(2L, 2L));

        PLL<Integer> union = first.concatenate(second);

        Assert.assertEquals(union.cachedPartitionSizes, Arrays.asList(2L, 2L, 2L, 2L));
        Assert.assertEquals(union.numPartitions(), 4);
        Assert.assertEquals(union.collect(), Arrays.asList(0, 1, 2, 3, 10, 11, 12, 13));
        String repr = union.toString();
        Assert.assertTrue(repr.contains("Union"));
    }
}
