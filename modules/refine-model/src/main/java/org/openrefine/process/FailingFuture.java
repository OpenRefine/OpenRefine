
package org.openrefine.process;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FailingFuture<T> implements ProgressingFuture<T> {

    final Exception cause;

    public FailingFuture(Exception cause) {
        this.cause = cause;
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
        return 0;
    }

    @Override
    public void onProgress(ProgressReporter reporter) {
        reporter.reportProgress(0, 0L, 0L);
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
        throw new ExecutionException(cause);
    }

    @Override
    public T get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new ExecutionException(cause);
    }

    @Override
    public void addListener(Runnable listener, Executor executor) {
        executor.execute(listener);
    }
}
