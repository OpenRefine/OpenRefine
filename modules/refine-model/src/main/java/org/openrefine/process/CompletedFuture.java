
package org.openrefine.process;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An implementation of {@link ProgressingFuture} which is already supplied with its final value. Therefore, there is
 * nothing to compute and methods provided to manage the computation process do nothing.
 */
public class CompletedFuture<T> implements ProgressingFuture<T> {

    final T value;

    /**
     * Constructs a future whose value is already computed.
     */
    public CompletedFuture(T value) {
        this.value = value;
    }

    @Override
    public void pause() {
        // does nothing
    }

    @Override
    public void resume() {
        // does nothing
    }

    @Override
    public boolean isPaused() {
        return false;
    }

    @Override
    public boolean supportsProgress() {
        return false;
    }

    @Override
    public int getProgress() {
        return 100;
    }

    @Override
    public void onProgress(ProgressReporter reporter) {
        reporter.reportProgress(100);
    }

    @Override
    public boolean cancel(boolean b) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return value;
    }

    @Override
    public T get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return value;
    }

    @Override
    public void addListener(Runnable listener, Executor executor) {
        executor.execute(listener);
    }
}
