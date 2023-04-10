
package org.openrefine.util;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import io.vavr.PartialFunction;
import io.vavr.Tuple2;
import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class CloseableIteratorTests {

    Iterator<Integer> source;
    AutoCloseable closeable;
    Iterator<Integer> source2;
    AutoCloseable closeable2;
    CloseableIterator<Integer> iterator;
    CloseableIterator<Integer> iterator2;

    @BeforeTest
    public void setUp() {
        source = mock(IteratorInt.class);
        closeable = mock(AutoCloseable.class);
        source2 = mock(IteratorInt.class);
        closeable2 = mock(AutoCloseable.class);
        iterator = new CloseableIterator.Wrapper<>(source, List.of(closeable));
        iterator2 = new CloseableIterator.Wrapper<>(source2, List.of(closeable2));
    }

    @Test
    public void testOf() {
        try (CloseableIterator<Integer> integers = CloseableIterator.of(3, 4)) {
            assertTrue(integers instanceof CloseableIterator.Wrapper);
            assertEquals(integers.toJavaList(), Arrays.asList(3, 4));
        }
    }

    @Test
    public void testOfAll() {
        try (CloseableIterator<Integer> integers = CloseableIterator.ofAll(Arrays.asList(5, 6))) {
            assertTrue(integers instanceof CloseableIterator.Wrapper);
            assertEquals(integers.toJavaList(), Arrays.asList(5, 6));
        }
    }

    @Test
    public void testEmpty() {
        try (CloseableIterator<Integer> empty = CloseableIterator.empty()) {
            assertTrue(empty instanceof CloseableIterator.Wrapper);
            assertEquals(empty.toJavaList(), Collections.emptyList());
        }
    }

    @Test
    public void testIntersperse() throws Exception {
        CloseableIterator<Integer> derived = iterator.intersperse(4);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testCollect() throws Exception {
        CloseableIterator<Integer> derived = iterator.collect(new PartialFunction<Integer, Integer>() {

            @Override
            public Integer apply(Integer integer) {
                return integer + 1;
            }

            @Override
            public boolean isDefinedAt(Integer value) {
                return true;
            }
        });
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testConcat() throws Exception {
        CloseableIterator<Integer> derived = iterator.concat(iterator2);
        derived.close();
        verify(closeable, atLeastOnce()).close();
        verify(closeable2, atLeastOnce()).close();
    }

    @Test
    public void testZip() throws Exception {
        CloseableIterator<Tuple2<Integer, Integer>> derived = iterator.zip(iterator2);
        derived.close();
        verify(closeable, atLeastOnce()).close();
        verify(closeable2, atLeastOnce()).close();
    }

    @Test
    public void testZipWith() throws Exception {
        CloseableIterator<Integer> derived = iterator.zipWith(iterator2, (x, y) -> y);
        derived.close();
        verify(closeable, atLeastOnce()).close();
        verify(closeable2, atLeastOnce()).close();
    }

    @Test
    public void testZipAll() throws Exception {
        CloseableIterator<Tuple2<Integer, Integer>> derived = iterator.zipAll(iterator2, 5, 7);
        derived.close();
        verify(closeable, atLeastOnce()).close();
        verify(closeable2, atLeastOnce()).close();
    }

    @Test
    public void testZipWithIndex() throws Exception {
        CloseableIterator<Tuple2<Integer, Integer>> derived = iterator.zipWithIndex();
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testZipWithIndexWithMapper() throws Exception {
        CloseableIterator<Integer> derived = iterator.zipWithIndex((x, y) -> x);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testDistinct() throws Exception {
        CloseableIterator<Integer> derived = iterator.distinct();
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testDistinctBy() throws Exception {
        CloseableIterator<Integer> derived = iterator.distinctBy(Comparator.naturalOrder());
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testDistinctByKeyExtractor() throws Exception {
        CloseableIterator<Integer> derived = iterator.distinctBy(x -> x);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testDrop() throws Exception {
        CloseableIterator<Integer> derived = iterator.drop(3);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testDropRight() throws Exception {
        CloseableIterator<Integer> derived = iterator.dropRight(3);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testDropUntil() throws Exception {
        CloseableIterator<Integer> derived = iterator.dropUntil(x -> true);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testDropWhile() throws Exception {
        CloseableIterator<Integer> derived = iterator.dropWhile(x -> false);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testFilter() throws Exception {
        CloseableIterator<Integer> derived = iterator.filter(x -> false);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testReject() throws Exception {
        CloseableIterator<Integer> derived = iterator.filter(x -> true);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testGrouped() throws Exception {
        when(source.hasNext()).thenReturn(true);
        when(source.next()).thenReturn(8);
        CloseableIterator<Seq<Integer>> derived = iterator.grouped(4);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testInit() throws Exception {
        when(source.hasNext()).thenReturn(true);
        when(source.next()).thenReturn(8);
        CloseableIterator<Integer> derived = iterator.init();
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testMap() throws Exception {
        CloseableIterator<Integer> derived = iterator.map(x -> x + 1);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testOrElseEmpty() throws Exception {
        when(source.hasNext()).thenReturn(false);
        CloseableIterator<Integer> derived = iterator.orElse(iterator2);
        derived.close();
        verify(closeable, atLeastOnce()).close();
        verify(closeable2, atLeastOnce()).close();
    }

    @Test
    public void testOrElseNonEmpty() throws Exception {
        when(source.hasNext()).thenReturn(true);
        CloseableIterator<Integer> derived = iterator.orElse(iterator2);
        derived.close();
        verify(closeable, atLeastOnce()).close();
        verify(closeable2, atLeastOnce()).close();
    }

    @Test
    public void testPeek() throws Exception {
        CloseableIterator<Integer> derived = iterator.peek(x -> {
        });
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testReplace() throws Exception {
        CloseableIterator<Integer> derived = iterator.replace(5, 9);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testReplaceAll() throws Exception {
        CloseableIterator<Integer> derived = iterator.replaceAll(5, 9);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testRetainAll() throws Exception {
        CloseableIterator<Integer> derived = iterator.retainAll(Arrays.asList(9, 78));
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testScanLeft() throws Exception {
        CloseableIterator<String> derived = iterator.scanLeft("", (s, i) -> String.format("%s%d", s, i));
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testScanRight() throws Exception {
        when(source.hasNext()).thenReturn(false);
        CloseableIterator<String> derived = iterator.scanRight("", (i, s) -> String.format("%s%d", s, i));
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testSlideBy() throws Exception {
        CloseableIterator<Seq<Integer>> derived = iterator.slideBy(x -> x);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testSliding() throws Exception {
        CloseableIterator<Seq<Integer>> derived = iterator.sliding(3);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testSlidingStep() throws Exception {
        CloseableIterator<Seq<Integer>> derived = iterator.sliding(3, 2);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testTail() throws Exception {
        when(source.hasNext()).thenReturn(true);
        CloseableIterator<Integer> derived = iterator.tail();
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testTake() throws Exception {
        CloseableIterator<Integer> derived = iterator.take(4);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testTakeRight() throws Exception {
        CloseableIterator<Integer> derived = iterator.takeRight(2);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testTakeUntil() throws Exception {
        CloseableIterator<Integer> derived = iterator.takeUntil(x -> true);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testTakeWhile() throws Exception {
        CloseableIterator<Integer> derived = iterator.takeWhile(x -> false);
        derived.close();
        verify(closeable, atLeastOnce()).close();
    }

    @Test
    public void testFlatMap() throws Exception {
        Iterator<CloseableIterator<Integer>> sourceIterator = mock(IteratorIteratorInt.class);
        when(sourceIterator.hasNext()).thenReturn(true);
        CloseableIterator<Integer> childIterator = mock(CloseableIteratorInt.class);
        when(sourceIterator.next()).thenReturn(childIterator);
        when(childIterator.hasNext()).thenReturn(true);
        when(childIterator.next()).thenReturn(9);

        CloseableIterator<Integer> flatMapped = new CloseableIterator.Wrapper<>(sourceIterator, List.of(closeable))
                .flatMapCloseable(t -> t);

        assertTrue(flatMapped.hasNext());
        assertEquals(flatMapped.next(), 9);
        flatMapped.close();
        verify(childIterator, atLeastOnce()).close();
    }

    // for mocking purposes
    public interface IteratorInt extends Iterator<Integer> {

    }

    public interface IteratorIteratorInt extends Iterator<CloseableIterator<Integer>> {

    }

    public interface CloseableIteratorInt extends CloseableIterator<Integer> {

    }
}
