
package org.openrefine.model.local;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.model.local.partitioning.LongRangePartitioner;

public class InMemoryPLLTests extends PLLTestsBase {

    List<Integer> list = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

    InMemoryPLL<Integer> SUT;
    InMemoryPLL<Integer> emptySUT;

    @BeforeTest
    public void setUpPLL() {
        SUT = new InMemoryPLL<Integer>(context, list, 4);
        emptySUT = new InMemoryPLL<Integer>(context, Collections.emptyList(), 4);
    }

    @Test
    public void testCount() {
        Assert.assertEquals(SUT.count(), 10L);
        Assert.assertEquals(emptySUT.count(), 0L);
    }

    @Test
    public void testCollect() {
        Assert.assertEquals(SUT.collect(), list);
        Assert.assertEquals(emptySUT.collect(), Collections.emptyList());
    }

    @Test
    public void testIterate() {
        Assert.assertEquals(SUT.stream().collect(Collectors.toList()), list);
        Assert.assertFalse(emptySUT.stream().iterator().hasNext());
    }

    @Test
    public void testIsEmpty() {
        Assert.assertFalse(SUT.isEmpty());
        Assert.assertTrue(emptySUT.isEmpty());
    }

    @Test
    public void testIsEmptyNoCachedCount() {
        // force discarding the cached counts with a spurious mapPartitions call
        Assert.assertFalse(SUT.mapPartitions((i, s) -> s, false).isEmpty());
        Assert.assertTrue(emptySUT.mapPartitions((i, s) -> s, false).isEmpty());
    }

    @Test
    public void testTake() {
        Assert.assertEquals(SUT.take(3), Arrays.asList(0, 1, 2));
        Assert.assertEquals(emptySUT.take(3), Collections.emptyList());
    }

    @Test
    public void testMap() {
        Assert.assertEquals(SUT.map(x -> 2 * x).collect(), Arrays.asList(0, 2, 4, 6, 8, 10, 12, 14, 16, 18));
    }

    @Test
    public void testFilter() {
        Assert.assertEquals(SUT.filter(x -> x % 2 == 0).collect(), Arrays.asList(0, 2, 4, 6, 8));
    }

    @Test
    public void testSort() {
        PLL<Integer> toSort = parallelize(4, Arrays.asList(4, 2, 7, 9, 8, 1, 0, 5, 6, 3));
        PLL<Integer> sorted = toSort.sort(Comparator.naturalOrder());
        Assert.assertEquals(sorted.collect(), SUT.collect());
        Assert.assertNotNull(sorted.cachedPartitionSizes);
    }

    @Test
    public void testZipWithIndex() {
        PairPLL<Long, Integer> indexed = SUT.zipWithIndex();
        Assert.assertTrue(indexed.getPartitioner().get() instanceof LongRangePartitioner);
        Assert.assertNotNull(indexed.cachedPartitionSizes);
        Assert.assertEquals(indexed.collect(), Arrays.asList(
                Tuple2.of(0L, 0),
                Tuple2.of(1L, 1),
                Tuple2.of(2L, 2),
                Tuple2.of(3L, 3),
                Tuple2.of(4L, 4),
                Tuple2.of(5L, 5),
                Tuple2.of(6L, 6),
                Tuple2.of(7L, 7),
                Tuple2.of(8L, 8),
                Tuple2.of(9L, 9)));
    }

    @Test
    public void testLimitPartitions() {
        PLL<Integer> limited = SUT.limitPartitions(2L);

        Assert.assertEquals(limited.collect(), Arrays.asList(
                0, 1, 3, 4, 6, 7, 8, 9));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testTakeNegative() {
        SUT.take(-3);
    }

    @Test
    public void testAggregate() {
        Assert.assertEquals((Integer) SUT.aggregate(0, (u, t) -> u + t, (u1, u2) -> u1 + u2), (Integer) 45);
        Assert.assertEquals((Integer) emptySUT.aggregate(0, (u, t) -> u + t, (u1, u2) -> u1 + u2), (Integer) 0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNoPartitions() {
        new InMemoryPLL<Integer>(context, list, 0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNegativePartitionNb() {
        new InMemoryPLL<Integer>(context, list, -3);
    }

    @Test
    public void testEmptyNoPartitions() {
        InMemoryPLL<Integer> noPartitions = new InMemoryPLL<Integer>(context, Collections.emptyList(), 0);
        Assert.assertEquals(noPartitions.count(), 0L);
        Assert.assertEquals(noPartitions.collect(), Collections.emptyList());
        Assert.assertFalse(noPartitions.stream().iterator().hasNext());
    }
}
