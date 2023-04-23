
package org.openrefine.util;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A collection which can be iterated on multiple times, using a {@link CloseableIterator}. This interface does not
 * extend {@link Iterable} because it would then make it possible to treat a {@link CloseableIterable} as an
 * {@link Iterable}, discarding the information that the derived iterators need closing. It is however possible to
 * represent any {@link Iterable} as a {@link CloseableIterable} using the {@link #of(Iterable)}.
 */
@FunctionalInterface
public interface CloseableIterable<T> {

    CloseableIterator<T> iterator();

    static <T> CloseableIterable<T> of(Iterable<T> iterable) {
        return new Wrapper(iterable);
    }

    static <T> CloseableIterable<T> empty() {
        return of(CloseableIterator::empty);
    }

    default CloseableIterable<T> drop(int n) {
        return () -> this.iterator().drop(n);
    }

    default CloseableIterable<T> take(int n) {
        return () -> this.iterator().take(n);
    }

    default <U> CloseableIterable<U> map(Function<? super T, ? extends U> function) {
        return () -> this.iterator().map(function);
    }

    default CloseableIterable<T> filter(Predicate<? super T> predicate) {
        return () -> this.iterator().filter(predicate);
    }

    class Wrapper<T> implements CloseableIterable<T> {

        private final Iterable<T> iterable;

        public Wrapper(Iterable<T> iterable) {
            this.iterable = iterable;
        }

        @Override
        public CloseableIterator<T> iterator() {
            return CloseableIterator.wrapping(iterable.iterator());
        }
    }
}
