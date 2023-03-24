
package org.openrefine.runners.local.pll.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.ListenableFuture;

import org.openrefine.process.ProgressReporter;
import org.openrefine.process.ProgressingFuture;

/**
 * A future which supports pausing and progress reporting, assuming the underlying future was built on a task that
 * relies on the supplied {@link TaskSignalling} object to periodically check for pause and report progress.
 */
public class ProgressingFutureWrapper<T> implements ProgressingFuture<T> {

    private final ListenableFuture<T> future;
    private final TaskSignalling taskSignalling;
    private final boolean supportsProgress;

    public ProgressingFutureWrapper(ListenableFuture<T> future, TaskSignalling taskSignalling, boolean supportsProgress) {
        this.future = future;
        this.taskSignalling = taskSignalling;
        this.supportsProgress = supportsProgress;
    }

    @Override
    public void pause() {
        taskSignalling.pause();
    }

    @Override
    public void resume() {
        taskSignalling.resume();
    }

    @Override
    public boolean isPaused() {
        return taskSignalling.isPaused();
    }

    @Override
    public boolean supportsProgress() {
        return supportsProgress;
    }

    @Override
    public int getProgress() {
        return taskSignalling.getProgress();
    }

    @Override
    public void onProgress(ProgressReporter reporter) {
        taskSignalling.registerProgressReporter(reporter);
    }

    @Override
    public boolean cancel(boolean b) {
        return future.cancel(b);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public T get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(l, timeUnit);
    }

    @Override
    public void addListener(Runnable listener, Executor executor) {
        future.addListener(listener, executor);
    }
}
