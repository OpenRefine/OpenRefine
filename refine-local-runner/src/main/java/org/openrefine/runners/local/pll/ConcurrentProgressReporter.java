
package org.openrefine.runners.local.pll;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import com.google.common.collect.Streams;

import org.openrefine.process.ProgressReporter;

/**
 * A helper to report iteration progress from concurrent threads.
 * 
 * @author Antonin Delpeuch
 *
 */
public class ConcurrentProgressReporter {

    long maxElements;
    ProgressReporter parentReporter;
    AtomicLong seenElements;

    public ConcurrentProgressReporter(ProgressReporter parentReporter, long maxElements) {
        this.maxElements = maxElements;
        this.parentReporter = parentReporter;
        this.seenElements = new AtomicLong(0L);
    }

    public void increment(long summand) {
        long seen = seenElements.addAndGet(summand);
        // TODO this is not fully thread-safe: the progress could be updated to a lower value
        // but that's not critical. It is probably not worth using a lock instead because of
        // the additional overhead
        parentReporter.reportProgress((int) ((100 * seen) / maxElements));
    }

    /**
     * Wraps a stream by updating the progress reporter when it is iterated from.
     * 
     * @param <T>
     * @param stream
     *            the stream to wrap
     * @param reportBatchSize
     *            the number of elements to wait for before updating the progress object
     * @param reportOffset
     *            the offset at which to start in the above batch
     * @return
     */
    protected <T> Stream<T> wrapStream(Stream<T> stream, int reportBatchSize, int reportOffset) {
        Iterator<T> iterator = new Iterator<T>() {

            Iterator<T> parent = stream.iterator();
            int seen = 0;

            @Override
            public boolean hasNext() {
                boolean hasNext = parent.hasNext();
                if (!hasNext && seen != 0) {
                    increment(seen);
                    seen = 0;
                }
                return hasNext;
            }

            @Override
            public T next() {
                seen++;
                T element = parent.next();
                if ((seen + reportOffset) % reportBatchSize == 0 && seen != 0) {
                    increment(seen);
                    seen = 0;
                }
                return element;
            }

        };
        return Streams.stream(iterator).onClose(() -> stream.close());
    }
}
