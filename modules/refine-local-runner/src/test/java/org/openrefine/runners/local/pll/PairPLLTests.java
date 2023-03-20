
package org.openrefine.runners.local.pll;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.openrefine.runners.local.pll.partitioning.Partitioner;
import org.openrefine.runners.local.pll.partitioning.PartitionerTestUtils;
import org.openrefine.runners.local.pll.partitioning.RangePartitioner;

public class PairPLLTests extends PLLTestsBase {

    PairPLL<Integer, String> SUT;
    PairPLL<Integer, String> noPartitionerSUT;

    @BeforeClass
    public void setUpPLL() {
        List<Tuple2<Integer, String>> list = Arrays.asList(
                Tuple2.of(1, "foo"),
                Tuple2.of(3, "bar"),
                Tuple2.of(2, "baz"),
                Tuple2.of(4, "hey"));

        Partitioner<Integer> partitioner = new Partitioner<Integer>() {

            @Override
            public int getPartition(Integer key) {
                return 1 - (key % 2);
            }

            @Override
            public int numPartitions() {
                return 2;
            }

        };

        PLL<Tuple2<Integer, String>> pll = new InMemoryPLL<Tuple2<Integer, String>>(context, list, 2);
        SUT = new PairPLL<Integer, String>(pll, Optional.of(partitioner));
        noPartitionerSUT = new PairPLL<Integer, String>(pll, Optional.empty());
    }

    @Test
    public void testToString() {
        Assert.assertTrue(SUT.toString().contains("PairPLL"));
    }

    @Test
    public void testKeys() {
        Assert.assertEquals(SUT.keys().collect(), Arrays.asList(1, 3, 2, 4));
    }

    @Test
    public void testValues() {
        Assert.assertEquals(SUT.values().collect(), Arrays.asList("foo", "bar", "baz", "hey"));
    }

    @Test
    public void testMapValues() {
        Assert.assertEquals(SUT.mapValues((i, s) -> s.charAt(0), "map description").collect(),
                Arrays.asList(Tuple2.of(1, 'f'), Tuple2.of(3, 'b'), Tuple2.of(2, 'b'), Tuple2.of(4, 'h')));
    }

    @Test
    public void testGetPartitioner() {
        Assert.assertTrue(SUT.getPartitioner().isPresent());
    }

    @Test
    public void testGet() {
        Assert.assertEquals(SUT.get(2), Collections.singletonList("baz"));
    }

    @Test
    public void testGetRangeAfterWithPartitioner() {
        PairPLL<Long, Integer> zipped = parallelize(4, Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17)).zipWithIndex();

        Assert.assertEquals(zipped.getRangeAfter(-1L, 2, Comparator.naturalOrder()),
                Arrays.asList(
                        Tuple2.of(0L, 10),
                        Tuple2.of(1L, 11)));

        Assert.assertEquals(zipped.getRangeAfter(7L, 2, Comparator.naturalOrder()),
                Arrays.asList(
                        Tuple2.of(7L, 17)));

        Assert.assertEquals(zipped.getRangeAfter(3L, 2, Comparator.naturalOrder()),
                Arrays.asList(
                        Tuple2.of(3L, 13),
                        Tuple2.of(4L, 14)));
    }

    @Test
    public void testGetRangeAfterWithoutPartitioner() {
        PairPLL<Long, Integer> zipped = new PairPLL<Long, Integer>(
                parallelize(4, Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17)).zipWithIndex().toPLL(), Optional.empty());

        Assert.assertEquals(zipped.getRangeAfter(-1L, 2, Comparator.naturalOrder()),
                Arrays.asList(
                        Tuple2.of(0L, 10),
                        Tuple2.of(1L, 11)));

        Assert.assertEquals(zipped.getRangeAfter(7L, 2, Comparator.naturalOrder()),
                Arrays.asList(
                        Tuple2.of(7L, 17)));

        Assert.assertEquals(zipped.getRangeAfter(3L, 2, Comparator.naturalOrder()),
                Arrays.asList(
                        Tuple2.of(3L, 13),
                        Tuple2.of(4L, 14)));
    }

    @Test
    public void testGetRangeBeforeWithPartitioner() {
        PairPLL<Long, Integer> zipped = parallelize(4, Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17)).zipWithIndex();

        Assert.assertEquals(zipped.getRangeBefore(3L, 2, Comparator.naturalOrder()),
                Arrays.asList(
                        Tuple2.of(1L, 11),
                        Tuple2.of(2L, 12)));

        Assert.assertEquals(zipped.getRangeBefore(1L, 2, Comparator.naturalOrder()),
                Arrays.asList(
                        Tuple2.of(0L, 10)));

        Assert.assertEquals(zipped.getRangeBefore(9L, 2, Comparator.naturalOrder()),
                Arrays.asList(
                        Tuple2.of(6L, 16),
                        Tuple2.of(7L, 17)));
    }

    @Test
    public void testGetRangeBeforeWithoutPartitioner() {
        PairPLL<Long, Integer> zipped = new PairPLL<>(
                parallelize(4, Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17)).zipWithIndex().toPLL(),
                Optional.empty());

        Assert.assertEquals(zipped.getRangeBefore(3L, 2, Comparator.naturalOrder()),
                Arrays.asList(
                        Tuple2.of(1L, 11),
                        Tuple2.of(2L, 12)));

        Assert.assertEquals(zipped.getRangeBefore(1L, 2, Comparator.naturalOrder()),
                Arrays.asList(
                        Tuple2.of(0L, 10)));

        Assert.assertEquals(zipped.getRangeBefore(9L, 2, Comparator.naturalOrder()),
                Arrays.asList(
                        Tuple2.of(6L, 16),
                        Tuple2.of(7L, 17)));
    }

    @Test
    public void testGetByKeysWithPartitioner() {
        PairPLL<Long, Integer> zipped = parallelize(4, Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19)).zipWithIndex();

        Assert.assertEquals(
                zipped.getByKeys(Collections.emptySet()),
                Collections.emptyList());

        Assert.assertEquals(
                zipped.getByKeys(Collections.singleton(4L)),
                Collections.singletonList(Tuple2.of(4L, 14)));

        Set<Long> keys = new HashSet<>();
        keys.add(3L);
        keys.add(4L);
        keys.add(9L);
        Assert.assertEquals(
                zipped.getByKeys(keys),
                Arrays.asList(Tuple2.of(3L, 13), Tuple2.of(4L, 14), Tuple2.of(9L, 19)));
    }

    @Test
    public void testGetByKeysWithoutPartitioner() {
        PairPLL<Long, Integer> zipped = new PairPLL<Long, Integer>(
                parallelize(4, Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19)).zipWithIndex().toPLL(), Optional.empty());

        Assert.assertEquals(
                zipped.getByKeys(Collections.emptySet()),
                Collections.emptyList());

        Assert.assertEquals(
                zipped.getByKeys(Collections.singleton(4L)),
                Collections.singletonList(Tuple2.of(4L, 14)));

        Set<Long> keys = new HashSet<>();
        keys.add(3L);
        keys.add(4L);
        keys.add(9L);
        Assert.assertEquals(
                zipped.getByKeys(keys),
                Arrays.asList(Tuple2.of(3L, 13), Tuple2.of(4L, 14), Tuple2.of(9L, 19)));
    }

    @Test
    public void testGetNotFound() {
        Assert.assertEquals(SUT.get(5), Collections.emptyList());
    }

    @Test
    public void testGetNoPartitioner() {
        Assert.assertEquals(noPartitionerSUT.get(2), Collections.singletonList("baz"));
    }

    @Test
    public void testAssumeIndexedPartitioner() {
        PairPLL<Long, Integer> indexed = parallelize(3, Arrays.asList(3, 8, 1, -3, 9, 10, 22, 15, 4, 5))
                .zipWithIndex();
        Assert.assertTrue(indexed.getPartitioner().isPresent());

        PairPLL<Long, Integer> doublyIndexed = PairPLL.assumeIndexed(indexed, -1);
        Assert.assertEquals(doublyIndexed.getPartitioner(), indexed.getPartitioner());
    }

    @Test
    public void testAssumeIndexedNoPartitionerNoCount() {
        PairPLL<Long, Integer> indexed = parallelize(3, Arrays.asList(3, 8, 1, -3, 9, 10, 22, 15, 4, 5))
                .zipWithIndex();
        List<Optional<Long>> expectedFirstKeys = ((RangePartitioner<Long>) indexed.getPartitioner().get()).getFirstKeys();
        // remove the partitioner and the cached count by adding a (spurious) filter
        indexed = indexed.withPartitioner(Optional.empty())
                .filter(x -> true);

        PairPLL<Long, Integer> doublyIndexed = PairPLL.assumeIndexed(indexed, -1);
        Assert.assertTrue(doublyIndexed.getPartitioner().get() instanceof RangePartitioner<?>);
        RangePartitioner<Long> partitioner = (RangePartitioner<Long>) doublyIndexed.getPartitioner().get();
        Assert.assertEquals(partitioner.getFirstKeys(), expectedFirstKeys);
        Assert.assertFalse(doublyIndexed.hasCachedPartitionSizes());
    }

    @Test
    public void testAssumeIndexedNoPartitionerCount() {
        PLL<Integer> list = parallelize(3, Arrays.asList(3, 8, 1, -3, 9, 10, 22, 15, 4, 5));
        List<Long> partitionSizes = list.getPartitionSizes();
        PairPLL<Long, Integer> indexed = list.zipWithIndex();
        List<Optional<Long>> expectedFirstKeys = ((RangePartitioner<Long>) indexed.getPartitioner().get()).getFirstKeys();
        indexed = indexed.withPartitioner(Optional.empty());

        PairPLL<Long, Integer> doublyIndexed = PairPLL.assumeIndexed(indexed, 10L);
        Assert.assertTrue(doublyIndexed.getPartitioner().get() instanceof RangePartitioner<?>);
        RangePartitioner<Long> partitioner = (RangePartitioner<Long>) doublyIndexed.getPartitioner().get();
        Assert.assertEquals(partitioner.getFirstKeys(), expectedFirstKeys);
        Assert.assertTrue(doublyIndexed.hasCachedPartitionSizes());
        Assert.assertEquals(doublyIndexed.getPartitionSizes(), partitionSizes);
    }

    @Test
    public void testAssumeSortedPartitioner() {
        PairPLL<Long, Integer> sorted = parallelize(3, Arrays.asList(3, 8, 1, -3, 9, 10, 22, 15, 4))
                .zipWithIndex()
                .filter(tuple -> tuple.getKey() % 2 == 0);
        Assert.assertTrue(sorted.getPartitioner().isPresent());

        PairPLL<Long, Integer> reSorted = PairPLL.assumeSorted(sorted);

        Assert.assertEquals(reSorted.getPartitioner(), sorted.getPartitioner());
    }

    @Test
    public void testAssumeSortedNoPartitioner() {
        PairPLL<Long, Integer> sorted = parallelize(3, Arrays.asList(3, 8, 1, -3, 9, 10, 22, 15, 4))
                .zipWithIndex()
                .filter(tuple -> tuple.getKey() % 2 == 0);
        Assert.assertTrue(sorted.getPartitioner().isPresent());
        List<Optional<Long>> expectedFirstKeys = ((RangePartitioner<Long>) sorted.getPartitioner().get()).getFirstKeys()
                .stream()
                .map(key -> key.isPresent() && key.get() % 2 == 0 ? key : Optional.of(key.get() + 1))
                .collect(Collectors.toList());
        sorted = sorted.withPartitioner(Optional.empty());

        PairPLL<Long, Integer> reSorted = PairPLL.assumeSorted(sorted);

        Assert.assertTrue(reSorted.getPartitioner().get() instanceof RangePartitioner<?>);
        RangePartitioner<Long> partitioner = (RangePartitioner<Long>) reSorted.getPartitioner().get();
        Assert.assertEquals(partitioner.getFirstKeys(), expectedFirstKeys);
    }

    @Test
    public void testWithCachedPartitionSizes() {
        PLL<Integer> list = parallelize(3, Arrays.asList(3, 8, 1, -3, 9, 10, 22, 15, 4));
        PairPLL<Long, Integer> indexed = list.zipWithIndex();
        List<Long> partitionSizes = indexed.getPartitionSizes();
        indexed = indexed.filter(tuple -> true); // spurious filter to remove partition sizes

        Assert.assertFalse(indexed.hasCachedPartitionSizes());
        PairPLL<Long, Integer> indexedWithPartitionSizes = indexed.withCachedPartitionSizes(partitionSizes);
        Assert.assertTrue(indexedWithPartitionSizes.hasCachedPartitionSizes());
        Assert.assertEquals(indexedWithPartitionSizes.getPartitionSizes(), partitionSizes);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWithInvalidCachedPartitionSizes() {
        PLL<Integer> list = parallelize(3, Arrays.asList(3, 8, 1, -3, 9, 10, 22, 15, 4));
        PairPLL<Long, Integer> indexed = list.zipWithIndex();
        indexed.withCachedPartitionSizes(Arrays.asList(2L, 3L));
    }

    @Test
    public void testDropLeft() {
        PLL<Integer> list = parallelize(3, Arrays.asList(3, 8, 1, -3, 9, 10, 22, 15, 4));
        PairPLL<Long, Integer> indexed = list.zipWithIndex();

        PairPLL<Long, Integer> dropped = indexed.dropFirstElements(5);
        Assert.assertEquals(dropped.collect(), Arrays.asList(Tuple2.of(5L, 10), Tuple2.of(6L, 22), Tuple2.of(7L, 15), Tuple2.of(8L, 4)));
        Assert.assertTrue(dropped.partitioner.isPresent());
        PartitionerTestUtils.checkPartitionerAdequacy(dropped.partitioner.get(), dropped);
    }

    @Test
    public void testDropRight() {
        PLL<Integer> list = parallelize(3, Arrays.asList(3, 8, 1, -3, 9, 10, 22, 15, 4));
        PairPLL<Long, Integer> indexed = list.zipWithIndex();

        PairPLL<Long, Integer> dropped = indexed.dropLastElements(5);
        Assert.assertEquals(dropped.collect(), Arrays.asList(Tuple2.of(0L, 3), Tuple2.of(1L, 8), Tuple2.of(2L, 1), Tuple2.of(3L, -3)));
        Assert.assertTrue(dropped.partitioner.isPresent());
        PartitionerTestUtils.checkPartitionerAdequacy(dropped.partitioner.get(), dropped);
    }

    @Test
    public void testGatherElementsBefore() {
        List<Tuple2<Long, Integer>> list = Arrays.asList(
                Tuple2.of(0L, 10),
                Tuple2.of(2L, 12),
                Tuple2.of(4L, 14),
                Tuple2.of(6L, 16),
                Tuple2.of(8L, 18));

        Assert.assertEquals(PairPLL.gatherElementsBefore(5L, 2, list.stream(), Comparator.naturalOrder()),
                list.subList(1, 3));
        Assert.assertEquals(PairPLL.gatherElementsBefore(13L, 2, list.stream(), Comparator.naturalOrder()),
                list.subList(3, 5));
        Assert.assertEquals(PairPLL.gatherElementsBefore(1L, 2, list.stream(), Comparator.naturalOrder()),
                list.subList(0, 1));
        Assert.assertEquals(PairPLL.gatherElementsBefore(10L, 20, list.stream(), Comparator.naturalOrder()),
                list);
    }
}
