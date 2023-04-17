
package org.openrefine.runners.local.pll;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DropPartitionsPLLTests extends PLLTestsBase {

    PLL<Integer> source;

    @BeforeMethod
    public void setUpSource() {
        source = parallelize(3, Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));
    }

    @Test
    public void testDrop() {
        PLL<Integer> dropped = source.retainPartitions(Arrays.asList(0, 2));

        assertEquals(dropped.collect(), Arrays.asList(0, 1, 2, 6, 7, 8));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalid() {
        source.retainPartitions(Arrays.asList(4));
    }

    @Test
    public void testReorder() {
        PLL<Integer> dropped = source.retainPartitions(Arrays.asList(1, 2, 0));

        assertEquals(dropped.collect(), Arrays.asList(3, 4, 5, 6, 7, 8, 0, 1, 2));
    }
}
