
package org.openrefine.runners.local.pll;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import io.vavr.collection.Iterator;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.util.CloseableIterator;

public class OrderedJoinPLLTests extends PLLTestsBase {

    private List<Tuple2<Long, String>> first;
    private List<Tuple2<Long, String>> second;

    @BeforeTest
    public void setUpPLLs() {
        first = Arrays.asList(
                Tuple2.of(2L, "foo"),
                Tuple2.of(4L, "bar"),
                Tuple2.of(5L, "boom"),
                Tuple2.of(6L, "hey"),
                Tuple2.of(8L, "you"));
        second = Arrays.asList(
                Tuple2.of(1L, "one"),
                Tuple2.of(4L, "four"),
                Tuple2.of(6L, "six"),
                Tuple2.of(7L, "seven"));
    }

    @Test
    public void testInnerJoinWithPartitioner() {
        PairPLL<Long, String> firstPLL = PairPLL.assumeSorted(
                parallelize(2, first)
                        .mapToPair(t -> t));
        PairPLL<Long, String> secondPLL = PairPLL.assumeSorted(
                parallelize(2, second)
                        .mapToPair(t -> t));

        PairPLL<Long, Tuple2<String, String>> joined = firstPLL.innerJoinOrdered(secondPLL, Comparator.naturalOrder());

        Assert.assertEquals(joined.collect(),
                Arrays.asList(
                        Tuple2.of(4L, Tuple2.of("bar", "four")),
                        Tuple2.of(6L, Tuple2.of("hey", "six"))));
    }

    @Test
    public void testInnerJoinWithoutPartitioner() {
        PairPLL<Long, String> firstPLL = parallelize(2, first)
                .mapToPair(t -> t);
        PairPLL<Long, String> secondPLL = parallelize(2, second)
                .mapToPair(t -> t);

        PairPLL<Long, Tuple2<String, String>> joined = firstPLL.innerJoinOrdered(secondPLL, Comparator.naturalOrder());

        Assert.assertEquals(joined.collect(),
                Arrays.asList(
                        Tuple2.of(4L, Tuple2.of("bar", "four")),
                        Tuple2.of(6L, Tuple2.of("hey", "six"))));
    }

    @Test
    public void testOuterJoinWithPartitioner() {
        PairPLL<Long, String> firstPLL = PairPLL.assumeSorted(
                parallelize(2, first)
                        .mapToPair(t -> t));
        PairPLL<Long, String> secondPLL = PairPLL.assumeSorted(
                parallelize(2, second)
                        .mapToPair(t -> t));

        PairPLL<Long, Tuple2<String, String>> joined = firstPLL.fullJoinOrdered(secondPLL, Comparator.naturalOrder());

        Assert.assertEquals(joined.collect(),
                Arrays.asList(
                        Tuple2.of(1L, Tuple2.of(null, "one")),
                        Tuple2.of(2L, Tuple2.of("foo", null)),
                        Tuple2.of(4L, Tuple2.of("bar", "four")),
                        Tuple2.of(5L, Tuple2.of("boom", null)),
                        Tuple2.of(6L, Tuple2.of("hey", "six")),
                        Tuple2.of(7L, Tuple2.of(null, "seven")),
                        Tuple2.of(8L, Tuple2.of("you", null))));
    }

    @Test
    public void testOuterJoinWithoutPartitioner() {
        PairPLL<Long, String> firstPLL = parallelize(2, first)
                .mapToPair(t -> t);
        PairPLL<Long, String> secondPLL = parallelize(2, second)
                .mapToPair(t -> t);

        PairPLL<Long, Tuple2<String, String>> joined = firstPLL.fullJoinOrdered(secondPLL, Comparator.naturalOrder());

        Assert.assertEquals(joined.collect(),
                Arrays.asList(
                        Tuple2.of(1L, Tuple2.of(null, "one")),
                        Tuple2.of(2L, Tuple2.of("foo", null)),
                        Tuple2.of(4L, Tuple2.of("bar", "four")),
                        Tuple2.of(5L, Tuple2.of("boom", null)),
                        Tuple2.of(6L, Tuple2.of("hey", "six")),
                        Tuple2.of(7L, Tuple2.of(null, "seven")),
                        Tuple2.of(8L, Tuple2.of("you", null))));
    }

    @Test
    public void testOuterJoinWithEmptyPartitions() {
        PairPLL<Long, String> firstPLL = parallelize(10, first)
                .mapToPair(t -> t);
        PairPLL<Long, String> secondPLL = parallelize(10, second)
                .mapToPair(t -> t);

        PairPLL<Long, Tuple2<String, String>> joined = firstPLL.fullJoinOrdered(secondPLL, Comparator.naturalOrder());

        Assert.assertEquals(joined.collect(),
                Arrays.asList(
                        Tuple2.of(1L, Tuple2.of(null, "one")),
                        Tuple2.of(2L, Tuple2.of("foo", null)),
                        Tuple2.of(4L, Tuple2.of("bar", "four")),
                        Tuple2.of(5L, Tuple2.of("boom", null)),
                        Tuple2.of(6L, Tuple2.of("hey", "six")),
                        Tuple2.of(7L, Tuple2.of(null, "seven")),
                        Tuple2.of(8L, Tuple2.of("you", null))));
    }

    @Test
    public void testMergeInnerEmptyStreams() {
        assertStreamsEqual(
                mergeInner(
                        seq(1, 2, 3),
                        seq()),
                joinedInner());

        assertStreamsEqual(
                mergeInner(
                        seq(),
                        seq(1, 2, 3)),
                joinedInner());

        assertStreamsEqual(
                mergeInner(
                        seq(),
                        seq()),
                joinedInner());
    }

    @Test
    public void testMergeInnerIdenticalStreams() {
        assertStreamsEqual(
                mergeInner(
                        seq(1, 2, 3),
                        seq(1, 2, 3)),
                joinedInner(1, 2, 3));
    }

    @Test
    public void testMergeInnerMismatchingStreams() {
        assertStreamsEqual(
                mergeInner(
                        seq(1, 3, 7),
                        seq(2, 4, 6)),
                joinedInner());
    }

    @Test
    public void testMergeInnerGenericStreams() {
        assertStreamsEqual(
                mergeInner(
                        seq(1, 3, 4, 7, 8),
                        seq(2, 3, 5, 6, 7)),
                joinedInner(3, 7));

        assertStreamsEqual(
                mergeInner(
                        seq(2, 3, 5, 6, 7),
                        seq(1, 3, 4, 7, 8)),
                joinedInner(3, 7));
    }

    @Test
    public void testMergeOuterEmptyStreams() {
        assertStreamsEqual(
                mergeOuter(
                        seq(1, 2, 3),
                        seq()),
                joinedOuter(new Integer[][] {
                        { 1, 1, null },
                        { 2, 2, null },
                        { 3, 3, null }
                }));

        assertStreamsEqual(
                mergeOuter(
                        seq(),
                        seq(1, 2, 3)),
                joinedOuter(new Integer[][] {
                        { 1, null, 1 },
                        { 2, null, 2 },
                        { 3, null, 3 }
                }));

        assertStreamsEqual(
                mergeOuter(
                        seq(),
                        seq()),
                joinedOuter());
    }

    @Test
    public void testMergeOuterIdenticalStreams() {
        assertStreamsEqual(
                mergeOuter(
                        seq(1, 2, 3),
                        seq(1, 2, 3)),
                joinedInner(1, 2, 3));
    }

    @Test
    public void testMergeOuterMismatchingStreams() {
        assertStreamsEqual(
                mergeOuter(
                        seq(1, 3, 7),
                        seq(2, 4, 6)),
                joinedOuter(new Integer[][] {
                        { 1, 1, null },
                        { 2, null, 2 },
                        { 3, 3, null },
                        { 4, null, 4 },
                        { 6, null, 6 },
                        { 7, 7, null }
                }));
    }

    @Test
    public void testMergeOuterGenericStreams() {
        assertStreamsEqual(
                mergeOuter(
                        seq(1, 3, 4, 7, 8),
                        seq(2, 3, 5, 6, 7)),
                joinedOuter(new Integer[][] {
                        { 1, 1, null },
                        { 2, null, 2 },
                        { 3, 3, 3 },
                        { 4, 4, null },
                        { 5, null, 5 },
                        { 6, null, 6 },
                        { 7, 7, 7 },
                        { 8, 8, null }
                }));

        assertStreamsEqual(
                mergeOuter(
                        seq(2, 3, 5, 6, 7),
                        seq(1, 3, 4, 7, 8)),
                joinedOuter(new Integer[][] {
                        { 1, null, 1 },
                        { 2, 2, null },
                        { 3, 3, 3 },
                        { 4, null, 4 },
                        { 5, 5, null },
                        { 6, 6, null },
                        { 7, 7, 7 },
                        { 8, null, 8 }
                }));
    }

    @Test
    public void testMergeLeftGenericStreams() {
        assertStreamsEqual(
                mergeLeft(
                        seq(1, 3, 4, 7, 8),
                        seq(2, 3, 5, 6, 7)),
                joinedOuter(new Integer[][] {
                        { 1, 1, null },
                        { 3, 3, 3 },
                        { 4, 4, null },
                        { 7, 7, 7 },
                        { 8, 8, null }
                }));

        assertStreamsEqual(
                mergeLeft(
                        seq(2, 3, 5, 6, 7),
                        seq(1, 3, 4, 7, 8)),
                joinedOuter(new Integer[][] {
                        { 2, 2, null },
                        { 3, 3, 3 },
                        { 5, 5, null },
                        { 6, 6, null },
                        { 7, 7, 7 },
                }));
    }

    @Test
    public void testMergeRightGenericStreams() {
        assertStreamsEqual(
                mergeRight(
                        seq(1, 3, 4, 7, 8),
                        seq(2, 3, 5, 6, 7)),
                joinedOuter(new Integer[][] {
                        { 2, null, 2 },
                        { 3, 3, 3 },
                        { 5, null, 5 },
                        { 6, null, 6 },
                        { 7, 7, 7 },
                }));

        assertStreamsEqual(
                mergeRight(
                        seq(2, 3, 5, 6, 7),
                        seq(1, 3, 4, 7, 8)),
                joinedOuter(new Integer[][] {
                        { 1, null, 1 },
                        { 3, 3, 3 },
                        { 4, null, 4 },
                        { 7, 7, 7 },
                        { 8, null, 8 }
                }));
    }

    protected CloseableIterator<Tuple2<Integer, Tuple2<Integer, Integer>>> mergeInner(CloseableIterator<Tuple2<Integer, Integer>> first,
            CloseableIterator<Tuple2<Integer, Integer>> second) {
        return OrderedJoinPLL.joinStreams(first, second, Comparator.<Integer> naturalOrder(), OrderedJoinPLL.JoinType.INNER);
    }

    protected CloseableIterator<Tuple2<Integer, Tuple2<Integer, Integer>>> mergeLeft(CloseableIterator<Tuple2<Integer, Integer>> first,
            CloseableIterator<Tuple2<Integer, Integer>> second) {
        return OrderedJoinPLL.joinStreams(first, second, Comparator.<Integer> naturalOrder(), OrderedJoinPLL.JoinType.LEFT);
    }

    protected CloseableIterator<Tuple2<Integer, Tuple2<Integer, Integer>>> mergeRight(CloseableIterator<Tuple2<Integer, Integer>> first,
            CloseableIterator<Tuple2<Integer, Integer>> second) {
        return OrderedJoinPLL.joinStreams(first, second, Comparator.<Integer> naturalOrder(), OrderedJoinPLL.JoinType.RIGHT);
    }

    protected CloseableIterator<Tuple2<Integer, Tuple2<Integer, Integer>>> mergeOuter(CloseableIterator<Tuple2<Integer, Integer>> first,
            CloseableIterator<Tuple2<Integer, Integer>> second) {
        return OrderedJoinPLL.joinStreams(first, second, Comparator.<Integer> naturalOrder(), OrderedJoinPLL.JoinType.FULL);
    }

    protected static <T> void assertStreamsEqual(CloseableIterator<T> actual, CloseableIterator<T> expected) {
        Assert.assertEquals(
                actual.collect(Collectors.toList()),
                expected.collect(Collectors.toList()));
    }

    protected CloseableIterator<Tuple2<Integer, Integer>> seq(Integer... elements) {
        return CloseableIterator.wrapping(Iterator.of(elements).map(i -> Tuple2.of(i, i)));
    }

    protected CloseableIterator<Tuple2<Integer, Tuple2<Integer, Integer>>> joinedInner(Integer... elements) {
        return CloseableIterator.wrapping(Iterator.of(elements).map(i -> Tuple2.of(i, Tuple2.of(i, i))));
    }

    protected CloseableIterator<Tuple2<Integer, Tuple2<Integer, Integer>>> joinedOuter(Integer[]... elements) {
        return CloseableIterator.wrapping(Iterator.of(elements).map(i -> Tuple2.of(i[0], Tuple2.of(i[1], i[2]))));
    }
}
