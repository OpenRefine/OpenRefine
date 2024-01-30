
package org.openrefine.runners.local.pll.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openrefine.process.ProgressReporter;
import org.openrefine.util.CloseableIterator;

/**
 * Utility class for a future and the underlying thread to communicate about pause and resume actions from the user, as
 * well as reporting progress. The computing thread is supposed to regularly call {@link #yield()} when it makes sense
 * for it to pause its work if needed.
 */
public class TaskSignalling {

    private boolean paused = false;
    private int progress = 0;
    private long totalElements;
    private long processedElements = 0;
    private List<ProgressReporter> progressReporters = new ArrayList<>();

    /**
     * Constructor.
     * 
     * @param totalElements
     *          the number of elements expected to be processed by the underlying task (used to compute process percentages)
     */
    public TaskSignalling(long totalElements) {
        this.totalElements = totalElements;
    }

    /**
     * Instruct the computing thread to pause its work. This will only happen once the computing thread calls
     * {@link #yield()}.
     */
    public synchronized void pause() {
        paused = true;
    }

    /**
     * Instruct the computing thread to resume its work if it was paused.
     */
    public synchronized void resume() {
        paused = false;
        notifyAll();
    }

    /**
     * Checks whether the thread is currently meant to be paused.
     */
    public synchronized boolean isPaused() {
        return paused;
    }

    /**
     * Adds a progress reporter which should be notified whenever the progress of this task changes.
     */
    public synchronized void registerProgressReporter(ProgressReporter reporter) {
        progressReporters.add(reporter);
        reporter.reportProgress(progress, processedElements, totalElements);
    }

    /**
     * Retrieves the current progress of the task.
     */
    public synchronized int getProgress() {
        return progress;
    }

    /**
     * Method to be called by the computing thread at various points when it makes sense for it to pause its work.
     * 
     * @throws InterruptedException
     *             thrown when the waiting stopped because the thread was interrupted (meaning that the task is
     *             canceled)
     */
    public synchronized void yield() throws InterruptedException {
        while (paused) {
            wait();
        }
    }

    /**
     * Method to be called by the computing thread after processing a bunch of elements, so that progress is updated.
     */
    public synchronized void addProcessedElements(long processedElements) {
        if (totalElements <= 0 || progress >= 100) {
            return;
        }
        this.processedElements += processedElements;
        progress = (int) ((100 * this.processedElements) / totalElements);
        broadcastProgress();
    }

    /**
     * Mark this task as fully done.
     */
    public synchronized void setFullProgress() {
        processedElements = totalElements;
        progress = 100;
        broadcastProgress();
    }

    private void broadcastProgress() {
        for (ProgressReporter reporter : progressReporters) {
            reporter.reportProgress(progress, processedElements, totalElements);
        }
    }

    /**
     * Wraps a stream by updating the progress reporter when it is iterated from.
     * It also checks if the thread should be paused after processing each element.
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
    public <T> CloseableIterator<T> wrapStream(CloseableIterator<T> stream, int reportBatchSize, int reportOffset) {
        CloseableIterator<T> iterator = new CloseableIterator<T>() {

            Iterator<T> parent = stream.iterator();
            int seen = 0;

            @Override
            public boolean hasNext() {
                try {
                    yield();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                boolean hasNext = parent.hasNext();
                if (!hasNext && seen != 0) {
                    addProcessedElements(seen);
                    seen = 0;
                }
                return hasNext;
            }

            @Override
            public T next() {
                seen++;
                T element = parent.next();
                if ((seen + reportOffset) % reportBatchSize == 0 && seen != 0) {
                    addProcessedElements(seen);
                    seen = 0;
                }
                return element;
            }

            @Override
            public void close() {
                stream.close();
            }
        };
        return iterator;
    }

}
