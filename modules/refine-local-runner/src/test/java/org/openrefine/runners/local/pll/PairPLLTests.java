
package org.openrefine.runners.local.pll;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.vavr.collection.Array;
import io.vavr.collection.Iterator;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.openrefine.runners.local.pll.partitioning.Partitioner;
import org.openrefine.runners.local.pll.partitioning.PartitionerTestUtils;
import org.openrefine.runners.local.pll.partitioning.RangePartitioner;
import org.openrefine.runners.local.pll.util.IterationContext;
import org.openrefine.util.CloseableIterable;
import org.openrefine.util.CloseableIterator;

public class PairPLLTests extends PLLTestsBase {

    PairPLL<Integer, String> SUT;
    PairPLL<Integer, String> noPartitionerSUT;
    PairPLL<Integer, String> noPartitionsSUT;

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

        PLL<Tuple2<Integer, String>> emptyPLL = new PLL<>(context, "empty PLL") {

            @Override
            protected CloseableIterator<Tuple2<Integer, String>> compute(Partition partition, IterationContext context) {
                throw new IllegalArgumentException("This PLL has no partitions");
            }

            @Override
            public Array<? extends Partition> getPartitions() {
                return Array.empty();
            }

            @Override
            public List<PLL<?>> getParents() {
                return Collections.emptyList();
            }
        };
        noPartitionsSUT = new PairPLL<>(emptyPLL, Optional.empty());
    }

    @Test
    public void testToString() {
        Assert.assertTrue(SUT.toString().contains("PairPLL"));
    }

    @Test
    public void testKeys() {
        Assert.assertEquals(SUT.keys().collect(), Arrays.asList(1, 3, 2, 4));
        Assert.assertEquals(noPartitionsSUT.keys().collect(), Collections.emptyList());
    }

    @Test
    public void testValues() {
        Assert.assertEquals(SUT.values().collect(), Arrays.asList("foo", "bar", "baz", "hey"));
        Assert.assertEquals(noPartitionsSUT.values().collect(), Collections.emptyList());
    }

    @Test
    public void testMapValues() {
        Assert.assertEquals(SUT.mapValues((i, s) -> s.charAt(0), "map description").collect(),
                Arrays.asList(Tuple2.of(1, 'f'), Tuple2.of(3, 'b'), Tuple2.of(2, 'b'), Tuple2.of(4, 'h')));
    }

    @Test
    public void testGetPartitioner() {
        Assert.assertTrue(SUT.getPartitioner().isPresent());
        Assert.assertFalse(noPartitionsSUT.getPartitioner().isPresent());
    }

    @Test
    public void testGet() {
        Assert.assertEquals(SUT.get(2), Collections.singletonList("baz"));
        Assert.assertEquals(noPartitionsSUT.get(2), Collections.emptyList());
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
        Array<Long> partitionSizes = list.getPartitionSizes();
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
        Array<Long> partitionSizes = indexed.getPartitionSizes();
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
        indexed.withCachedPartitionSizes(Array.of(2L, 3L));
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

        Assert.assertEquals(PairPLL.gatherElementsBefore(5L, 2, CloseableIterator.wrapping(list.iterator()), Comparator.naturalOrder()),
                list.subList(1, 3));
        Assert.assertEquals(PairPLL.gatherElementsBefore(13L, 2, CloseableIterator.wrapping(list.iterator()), Comparator.naturalOrder()),
                list.subList(3, 5));
        Assert.assertEquals(PairPLL.gatherElementsBefore(1L, 2, CloseableIterator.wrapping(list.iterator()), Comparator.naturalOrder()),
                list.subList(0, 1));
        Assert.assertEquals(PairPLL.gatherElementsBefore(10L, 20, CloseableIterator.wrapping(list.iterator()), Comparator.naturalOrder()),
                list);
    }

    @Test
    public void testRetainPartitions() {
        List<Tuple2<Integer, String>> list = Arrays.asList(
                Tuple2.of(1, "foo"),
                Tuple2.of(3, "bar"),
                Tuple2.of(2, "baz"),
                Tuple2.of(4, "hey"));

        PLL<Tuple2<Integer, String>> pll = new InMemoryPLL<Tuple2<Integer, String>>(context, list, 2);
        PairPLL<Integer, String> pairPLL = new PairPLL<>(pll, Optional.empty());

        PairPLL<Integer, String> dropped = pairPLL.retainPartitions(Collections.singletonList(1));
        Assert.assertEquals(dropped.collect(), list.subList(2, 4));
    }

    @Test
    public void testGetUsingSortedPLL() {
        // we create an infinite PLL containing elements for all integers
        CloseableIterable<Tuple2<Long, String>> iterable = () -> CloseableIterator.wrapping(
                Iterator.from(0L).map(idx -> Tuple2.of(idx, "foo")));
        PairPLL<Long, String> pll = context.singlePartitionPLL(iterable, -1L)
                .mapToPair(tuple -> tuple);

        // we know it is sorted, so we mark it as such
        pll = PairPLL.assumeSorted(pll);

        // We can fetch a prefix of the stream without problem
        List<Tuple2<Long, String>> firstFew = pll.getRangeAfter(0L, 2, Comparator.naturalOrder());
        Assert.assertEquals(firstFew, Array.of(Tuple2.of(0L, "foo"), Tuple2.of(1L, "foo")));

        // But we can do more! Because it is sorted, we can actually look up elements in it: the search will stop as
        // soon as we encounter a key that is greater than the one looked up.
        Array<String> actual = pll.get(4L);
        Assert.assertEquals(actual, Array.of("foo"));
    }
}
