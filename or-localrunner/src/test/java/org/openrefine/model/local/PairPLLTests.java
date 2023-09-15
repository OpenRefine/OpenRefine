
package org.openrefine.model.local;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.model.local.partitioning.Partitioner;
import org.openrefine.model.local.partitioning.RangePartitioner;

public class PairPLLTests extends PLLTestsBase {

    PairPLL<Integer, String> SUT;
    PairPLL<Integer, String> noPartitionerSUT;

    @BeforeTest
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
    public void testKeys() {
        Assert.assertEquals(SUT.keys().collect(), Arrays.asList(1, 3, 2, 4));
    }

    @Test
    public void testValues() {
        Assert.assertEquals(SUT.values().collect(), Arrays.asList("foo", "bar", "baz", "hey"));
    }

    @Test
    public void testMapValues() {
        Assert.assertEquals(SUT.mapValues((i, s) -> s.charAt(0)).collect(),
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
    public void testGetRangeWithPartitioner() {
        PairPLL<Long, Integer> zipped = parallelize(4, Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17)).zipWithIndex();

        Assert.assertEquals(zipped.getRange(-1L, 2, Comparator.naturalOrder()),
                Arrays.asList(
                        Tuple2.of(0L, 10),
                        Tuple2.of(1L, 11)));

        Assert.assertEquals(zipped.getRange(7L, 2, Comparator.naturalOrder()),
                Arrays.asList(
                        Tuple2.of(7L, 17)));

        Assert.assertEquals(zipped.getRange(3L, 2, Comparator.naturalOrder()),
                Arrays.asList(
                        Tuple2.of(3L, 13),
                        Tuple2.of(4L, 14)));
    }

    @Test
    public void testGetRangeWithoutPartitioner() {
        PairPLL<Long, Integer> zipped = new PairPLL<Long, Integer>(
                parallelize(4, Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17)).zipWithIndex().toPLL(), Optional.empty());

        Assert.assertEquals(zipped.getRange(-1L, 2, Comparator.naturalOrder()),
                Arrays.asList(
                        Tuple2.of(0L, 10),
                        Tuple2.of(1L, 11)));

        Assert.assertEquals(zipped.getRange(7L, 2, Comparator.naturalOrder()),
                Arrays.asList(
                        Tuple2.of(7L, 17)));

        Assert.assertEquals(zipped.getRange(3L, 2, Comparator.naturalOrder()),
                Arrays.asList(
                        Tuple2.of(3L, 13),
                        Tuple2.of(4L, 14)));
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
        indexed = indexed.withPartitioner(Optional.empty());

        PairPLL<Long, Integer> doublyIndexed = PairPLL.assumeIndexed(indexed, -1);
        Assert.assertTrue(doublyIndexed.getPartitioner().get() instanceof RangePartitioner<?>);
        RangePartitioner<Long> partitioner = (RangePartitioner<Long>) doublyIndexed.getPartitioner().get();
        Assert.assertEquals(partitioner.getFirstKeys(), expectedFirstKeys);
        Assert.assertNull(doublyIndexed.cachedPartitionSizes);
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
        Assert.assertEquals(doublyIndexed.cachedPartitionSizes, partitionSizes);
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
}
