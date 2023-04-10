
package org.openrefine.runners.local.pll;

import static org.testng.Assert.assertNotEquals;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.runners.local.pll.PLL.PLLExecutionError;
import org.openrefine.util.CloseableIterator;

public class MapPartitionsPLLTests extends PLLTestsBase {

    List<Integer> list = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7);
    PLL<Integer> parent;

    @BeforeMethod
    public void setUpParent() {
        parent = new InMemoryPLL<Integer>(context, list, 2);
    }

    @Test
    public void testDouble() {
        MapPartitionsPLL<Integer, Integer> SUT = new MapPartitionsPLL<Integer, Integer>(parent, (i, t) -> t.take(i + 1), "description");

        Assert.assertEquals(SUT.collect(), Arrays.asList(0, 4, 5));
    }

    @Test(expectedExceptions = PLLExecutionError.class)
    public void testThrowsException() {
        BiFunction<Integer, CloseableIterator<Integer>, CloseableIterator<Integer>> faultyMap = ((a, b) -> {
            throw new UncheckedIOException("error", new IOException("e"));
        });

        MapPartitionsPLL<Integer, Integer> SUT = new MapPartitionsPLL<Integer, Integer>(parent, faultyMap, "description");

        SUT.collect();
    }

    @Test
    public void testId() {
        MapPartitionsPLL<Integer, Integer> SUT = new MapPartitionsPLL<Integer, Integer>(parent, (i, t) -> t.take(i + 1), "description");
        assertNotEquals(SUT.getId(), parent.getId());
    }
}
