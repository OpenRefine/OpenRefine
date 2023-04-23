
package org.openrefine.util;

import java.util.Comparator;
import java.util.function.*;

import io.vavr.PartialFunction;
import io.vavr.Tuple2;
import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.collection.Seq;

/**
 * An iterator which may be derived from resources that should be closed once the iterator is no longer needed. This is
 * used in the local runner to make it possible to iterate from partitions stored on disk and close the file descriptors
 * as soon as iteration stops. <br>
 * The Java 8 Stream API is often recommended as a closeable "iterator-like" API but in our case it cannot be used
 * because <a href=
 * "https://stackoverflow.com/questions/47036993/why-the-tryadvance-of-stream-spliterator-may-accumulate-items-into-a-buffer">in
 * some circumstances it buffers the stream</a>, forcing the loading of an entire partition in memory while only the
 * first few rows in it are actually needed. <br>
 * Like in the Java 8 Stream API, some methods return a new iterator and some others are a terminal action. Users should
 * only close the iterator after using a terminal action, not after using a method that returns a new iterator. The
 * derived iterator itself should be closed when a terminal action is called on it, and that will recursively call the
 * {@link #close()} method of the original iterator. Methods which derive an iterator from multiple iterators will close
 * all the underlying iterators when they are closed themselves. For this reason, one should not derive multiple
 * iterators from the same iterator, as they are using the same underlying resources.
 *
 * @param <T>
 *            the type of element being iterated on
 */
public interface CloseableIterator<T> extends Iterator<T>, AutoCloseable {

    /**
     * Checks if the iterator contains a next element. If this returns false, the iterator should already be closed: it
     * is not necessary to call {@link #close()} after that.
     */
    @Override
    boolean hasNext();

    /**
     * Returns the next element in the stream if there is one available. This may not be called after calling
     * {@link #close()}.
     */
    @Override
    T next();

    /**
     * Releases all resources underpinning this iterator. Calling this method multiple times is allowed but calling it
     * only once is sufficient. The {@link #next()} method may not be called after this one is called.
     */
    @Override
    void close();

    /**
     * A wrapper which takes a plain iterator and turns it into a closeable one. A list of closeable resources can be
     * supplied: when this iterator is closed, all those closeables will be closed as well.
     */
    public static class Wrapper<T> implements CloseableIterator<T> {

        private final Iterator<T> iterator;
        private boolean closed = false;
        private final List<AutoCloseable> toClose;

        /**
         * Wraps a plain iterator, supplying a list of closeables to be closed when the resulting
         * {@link CloseableIterator} is closed.
         */
        public Wrapper(Iterator<T> iterator, List<AutoCloseable> toClose) {
            this.iterator = iterator;
            this.toClose = toClose;
        }

        /**
         * Convenience constructor when there is only one underlying closeable.
         */
        public Wrapper(Iterator<T> iterator, AutoCloseable toClose) {
            this.iterator = iterator;
            this.toClose = List.of(toClose);
        }

        @Override
        public void close() {
            try {
                if (!closed) {
                    for (AutoCloseable closeable : toClose) {
                        try {
                            closeable.close();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            } finally {
                closed = true;
            }
        }

        @Override
        public boolean hasNext() {
            if (closed) {
                return false;
            }
            boolean hasNext = iterator.hasNext();
            try {
                return hasNext;
            } finally {
                if (!hasNext && !closed) {
                    close();
                }
            }
        }

        @Override
        public T next() {
            return iterator.next();
        }
    }

    static <T> CloseableIterator<T> of(T... elements) {
        return new Wrapper(Iterator.of(elements), List.empty());
    }

    static <T> CloseableIterator<T> ofAll(Iterable<? extends T> ts) {
        return new Wrapper<>(Iterator.ofAll(ts), List.empty());
    }

    static <T> CloseableIterator<T> empty() {
        return new Wrapper(Iterator.empty(), List.empty());
    }

    static CloseableIterator<Long> from(long start) {
        return new Wrapper(Iterator.from(start), List.empty());
    }

    static <T> CloseableIterator<T> wrapping(Iterator<? extends T> ts) {
        return new Wrapper(ts, List.empty());
    }

    static <T> CloseableIterator<T> wrapping(java.util.Iterator<? extends T> ts) {
        return new Wrapper<>(Iterator.ofAll(ts), List.empty());
    }

    static <T> CloseableIterator<T> wrapping(java.util.Iterator<? extends T> iterator, AutoCloseable closeable) {
        return new Wrapper(Iterator.ofAll(iterator), closeable);
    }

    @Override
    default <R> CloseableIterator<R> collect(PartialFunction<? super T, ? extends R> partialFunction) {
        return new Wrapper<>(Iterator.super.collect(partialFunction), this);
    }

    default CloseableIterator<T> concat(CloseableIterator<? extends T> that) {
        CloseableIterator<T> first = this;
        CloseableIterator<? extends T> second = that;
        return new CloseableIterator<T>() {

            @Override
            public void close() {
                first.close();
                second.close();
            }

            @Override
            public boolean hasNext() {
                return first.hasNext() || second.hasNext();
            }

            @Override
            public T next() {
                if (first.hasNext()) {
                    return first.next();
                } else {
                    return second.next();
                }
            }
        };
    }

    /**
     * Consumes the iterator by splitting it into chunks of the desired length. The returned iterators are not closeable
     * so the parent iterator should be closed directly. Also, the iterators returned should be consumed in the order
     * they are returned if the chunks are intended to be contiguous.
     *
     * @param lengths
     *            the lengths of the desired chunks
     */
    default Iterator<Iterator<T>> chop(Iterator<Integer> lengths) {
        return lengths.map(Iterator.super::take);
    }

    @Override
    default CloseableIterator<T> intersperse(T element) {
        return new Wrapper<T>(Iterator.super.intersperse(element), this);
    }

    /**
     * @deprecated This method should not be called as it discards the pointers to the closeable resources. This will
     *             lead to resource leaks.
     */
    @Override
    @Deprecated
    default <U> Iterator<Tuple2<T, U>> zip(Iterable<? extends U> that) {
        return Iterator.super.zip(that);
    }

    default <U> CloseableIterator<Tuple2<T, U>> zip(CloseableIterator<? extends U> that) {
        return new Wrapper<>(Iterator.super.zip(that), List.of(this, that));
    }

    /**
     * @deprecated This method should not be called as it discards the pointers to the closeable resources. This will
     *             lead to resource leaks.
     */
    @Override
    @Deprecated
    default <U, R> Iterator<R> zipWith(Iterable<? extends U> that, BiFunction<? super T, ? super U, ? extends R> mapper) {
        return Iterator.super.zipWith(that, mapper);
    }

    default <U, R> CloseableIterator<R> zipWith(CloseableIterator<? extends U> that, BiFunction<? super T, ? super U, ? extends R> mapper) {
        return new Wrapper<>(Iterator.super.zipWith(that, mapper), List.of(this, that));
    }

    /**
     * @deprecated This method should not be called as it discards the pointers to the closeable resources. This will
     *             lead to resource leaks.
     */
    @Override
    @Deprecated
    default <U> Iterator<Tuple2<T, U>> zipAll(Iterable<? extends U> that, T thisElem, U thatElem) {
        return Iterator.super.zipAll(that, thisElem, thatElem);
    }

    default <U> CloseableIterator<Tuple2<T, U>> zipAll(CloseableIterator<? extends U> that, T thisElem, U thatElem) {
        return new Wrapper<>(Iterator.super.zipAll(that, thisElem, thatElem), List.of(this, that));
    }

    @Override
    default CloseableIterator<Tuple2<T, Integer>> zipWithIndex() {
        return new Wrapper<>(Iterator.super.zipWithIndex(), this);
    }

    @Override
    default <U> CloseableIterator<U> zipWithIndex(BiFunction<? super T, ? super Integer, ? extends U> mapper) {
        return new Wrapper<>(Iterator.super.zipWithIndex(mapper), this);
    }

    @Override
    default CloseableIterator<T> distinct() {
        return new Wrapper<>(Iterator.super.distinct(), this);
    }

    @Override
    default CloseableIterator<T> distinctBy(Comparator<? super T> comparator) {
        return new Wrapper<>(Iterator.super.distinctBy(comparator), this);
    }

    @Override
    default <U> CloseableIterator<T> distinctBy(Function<? super T, ? extends U> keyExtractor) {
        return new Wrapper<>(Iterator.super.distinctBy(keyExtractor), this);
    }

    @Override
    default CloseableIterator<T> drop(int n) {
        return new Wrapper<>(Iterator.super.drop(n), this);
    }

    @Override
    default CloseableIterator<T> dropRight(int n) {
        return new Wrapper<>(Iterator.super.dropRight(n), this);
    }

    @Override
    default CloseableIterator<T> dropUntil(Predicate<? super T> predicate) {
        return new Wrapper<>(Iterator.super.dropUntil(predicate), this);
    }

    @Override
    default CloseableIterator<T> dropWhile(Predicate<? super T> predicate) {
        return new Wrapper<>(Iterator.super.dropWhile(predicate), this);
    }

    @Override
    default CloseableIterator<T> filter(Predicate<? super T> predicate) {
        return new Wrapper<>(Iterator.super.filter(predicate), this);
    }

    @Override
    default CloseableIterator<T> reject(Predicate<? super T> predicate) {
        return new Wrapper<>(Iterator.super.reject(predicate), this);
    }

    /**
     * @deprecated This method should not be called as it discards the pointers to the closeable resources. This will
     *             lead to resource leaks.
     */
    @Override
    @Deprecated
    default <U> Iterator<U> flatMap(Function<? super T, ? extends Iterable<? extends U>> mapper) {
        return Iterator.super.flatMap(mapper);
    }

    default <U> CloseableIterator<U> flatMapCloseable(Function<? super T, ? extends CloseableIterator<? extends U>> mapper) {
        CloseableIterator<T> parent = this;
        return new CloseableIterator<U>() {

            CloseableIterator<? extends U> current = null;

            @Override
            public void close() {
                if (current != null) {
                    current.close();
                }
                parent.close();
            }

            private void skipEmptySublists() {
                while ((current == null || !current.hasNext()) && parent.hasNext()) {
                    if (current != null) {
                        current.close();
                    }
                    current = mapper.apply(parent.next());
                }
            }

            @Override
            public boolean hasNext() {
                skipEmptySublists();
                return current != null && current.hasNext();
            }

            @Override
            public U next() {
                skipEmptySublists();
                return current.next();
            }
        };
    };

    @Override
    default CloseableIterator<Seq<T>> grouped(int size) {
        return new Wrapper<>(Iterator.super.grouped(size), this);
    }

    @Override
    default CloseableIterator<T> init() {
        return new Wrapper<>(Iterator.super.init(), this);
    }

    @Override
    default CloseableIterator<T> iterator() {
        return this;
    }

    @Override
    default <U> CloseableIterator<U> map(Function<? super T, ? extends U> mapper) {
        return new Wrapper<>(Iterator.super.map(mapper), this);
    }

    /**
     * @deprecated This method should not be called as it discards the pointers to the closeable resources. This will
     *             lead to resource leaks.
     */
    @Override
    @Deprecated
    default Iterator<T> orElse(Iterable<? extends T> other) {
        return Iterator.super.orElse(other);
    }

    default CloseableIterator<T> orElse(CloseableIterator<T> other) {
        return new Wrapper<>(isEmpty() ? other : this, List.of(this, other));
    }

    /**
     * @deprecated This method should not be called as it discards the pointers to the closeable resources. This will
     *             lead to resource leaks.
     */
    @Override
    @Deprecated
    default CloseableIterator<T> orElse(Supplier<? extends Iterable<? extends T>> supplier) {
        return isEmpty() ? ofAll(supplier.get()) : this;
    }

    @Override
    default CloseableIterator<T> peek(Consumer<? super T> action) {
        return new Wrapper<>(Iterator.super.peek(action), this);
    }

    @Override
    default CloseableIterator<T> replace(T currentElement, T newElement) {
        return new Wrapper<>(Iterator.super.replace(currentElement, newElement), this);
    }

    @Override
    default CloseableIterator<T> replaceAll(T currentElement, T newElement) {
        return new Wrapper<>(Iterator.super.replaceAll(currentElement, newElement), this);
    }

    @Override
    default CloseableIterator<T> retainAll(Iterable<? extends T> elements) {
        return new Wrapper<>(Iterator.super.retainAll(elements), this);
    }

    @Override
    default <U> CloseableIterator<U> scanLeft(U zero, BiFunction<? super U, ? super T, ? extends U> operation) {
        return new Wrapper<>(Iterator.super.scanLeft(zero, operation), this);
    }

    @Override
    default <U> CloseableIterator<U> scanRight(U zero, BiFunction<? super T, ? super U, ? extends U> operation) {
        return new Wrapper<>(Iterator.super.scanRight(zero, operation), this);
    }

    @Override
    default CloseableIterator<Seq<T>> slideBy(Function<? super T, ?> classifier) {
        return new Wrapper<>(Iterator.super.slideBy(classifier), this);
    }

    @Override
    default CloseableIterator<Seq<T>> sliding(int size) {
        return new Wrapper<>(Iterator.super.sliding(size), this);
    }

    @Override
    default CloseableIterator<Seq<T>> sliding(int size, int step) {
        return new Wrapper<>(Iterator.super.sliding(size, step), this);
    }

    @Override
    default CloseableIterator<T> tail() {
        return new Wrapper<>(Iterator.super.tail(), this);
    }

    @Override
    default CloseableIterator<T> take(int n) {
        return new Wrapper<>(Iterator.super.take(n), this);
    }

    @Override
    default CloseableIterator<T> takeRight(int n) {
        return new Wrapper<>(Iterator.super.takeRight(n), this);
    }

    @Override
    default CloseableIterator<T> takeUntil(Predicate<? super T> predicate) {
        return new Wrapper<>(Iterator.super.takeUntil(predicate), this);
    }

    @Override
    default CloseableIterator<T> takeWhile(Predicate<? super T> predicate) {
        return new Wrapper<>(Iterator.super.takeWhile(predicate), this);
    }

}
