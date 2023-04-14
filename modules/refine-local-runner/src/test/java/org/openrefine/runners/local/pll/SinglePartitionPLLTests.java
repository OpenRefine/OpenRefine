
package org.openrefine.runners.local.pll;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.util.CloseableIterable;
import org.openrefine.util.CloseableIterator;

public class SinglePartitionPLLTests extends PLLTestsBase {

    List<Integer> list = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

    CloseableIterable<Integer> iterable;
    PLL<Integer> pllSizeKnown;
    PLL<Integer> pllSizeUnknown;

    @BeforeMethod
    public void setUpPLLs() {
        iterable = spy(CloseableIterable.of(list));
        pllSizeKnown = new SinglePartitionPLL<>(context, iterable, 10L);
        pllSizeUnknown = new SinglePartitionPLL<>(context, iterable, -1L);
    }

    @Test
    public void testProperties() {
        assertEquals(pllSizeKnown.getPartitions().size(), 1);
        assertEquals(pllSizeUnknown.getPartitions().size(), 1);
        assertTrue(pllSizeKnown.hasCachedPartitionSizes());
        assertFalse(pllSizeUnknown.hasCachedPartitionSizes());
        assertEquals(pllSizeKnown.getParents(), Collections.emptyList());
        assertEquals(pllSizeUnknown.getParents(), Collections.emptyList());
        assertFalse(pllSizeKnown.isCached());
        assertFalse(pllSizeUnknown.isCached());
    }

    @Test
    public void testIterate() {
        try (CloseableIterator<Integer> iterator = pllSizeKnown.iterator()) {
            assertEquals(iterator.toJavaList(), list);
            verify(iterable, times(1)).iterator();
        }
    }

    @Test
    public void testCachedCount() {
        assertEquals(pllSizeKnown.count(), 10L);

        // the iterable was not iterated on because the count was cached
        verify(iterable, times(0)).iterator();
    }

    @Test
    public void testCountNotCached() {
        assertEquals(pllSizeUnknown.count(), 10L);

        // the iterable was iterated on because the count was not cached
        verify(iterable, times(1)).iterator();
    }

    interface CloseableIteratorInteger extends CloseableIterator<Integer> {
    }

    interface CloseableIterableInteger extends CloseableIterable<Integer> {
    }
}
