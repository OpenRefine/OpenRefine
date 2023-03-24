
package org.openrefine.process;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * Utilities around {@link ProgressingFuture}, added as static methods so as not to overload the interfaces with default
 * methods.
 */
public class ProgressingFutures {

    /**
     * A future whose result is already computed.
     */
    public static <T> ProgressingFuture<T> immediate(T value) {
        return new CompletedFuture<>(value);
    }

    /**
     * A precomputed future in an error state, defined by the supplied exception.
     */
    public static <T> ProgressingFuture<T> exception(Exception e) {
        return new FailingFuture<>(e);
    }

    /**
     * Wraps a listenable future into a progressing future which does not support reporting progress or pausing.
     */
    public static <T> ProgressingFuture<T> fromListenableFuture(ListenableFuture<T> future, Executor executor) {
        return new ProgressingFuture<T>() {

            List<ProgressReporter> reporters = new ArrayList<>();

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
                return future.isDone() ? 100 : 0;
            }

            @Override
            public void onProgress(ProgressReporter reporter) {
                reporter.reportProgress(getProgress());
                reporters.add(reporter);
                future.addListener(() -> reporter.reportProgress(100), executor);
            }

            @Override
            public void addListener(Runnable listener, Executor executor) {
                future.addListener(listener, executor);
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
                try {
                    return future.get();
                } finally {
                    for (ProgressReporter reporter : reporters) {
                        reporter.reportProgress(100);
                    }
                }
            }

            @Override
            public T get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
                try {
                    return future.get(l, timeUnit);
                } finally {
                    for (ProgressReporter reporter : reporters) {
                        reporter.reportProgress(100);
                    }
                }
            }
        };
    }

    /**
     * Chaining a future with a transformation.
     *
     * @param future
     *            the future containing the first computation in the chain
     * @param function
     *            the second computation to apply on top of the result of the future. This function must be relatively
     *            quick as no progress reporting or pausing for it will be supported by the returned future
     * @param executor
     *            the executor to use to schedule the execution of the second computation
     * @return a future which combines the two computations
     */
    public static <T, U> ProgressingFuture<U> transform(ProgressingFuture<T> future, Function<T, U> function,
            ListeningExecutorService executor) {
        ListenableFuture<U> transformed = Futures.transform(future, function, executor);
        return new ProgressingFuture<U>() {

            @Override
            public void pause() {
                future.pause();
            }

            @Override
            public void resume() {
                future.resume();
            }

            @Override
            public boolean isPaused() {
                return future.isPaused();
            }

            @Override
            public boolean supportsProgress() {
                return future.supportsProgress();
            }

            @Override
            public int getProgress() {
                return future.getProgress();
            }

            @Override
            public void onProgress(ProgressReporter reporter) {
                future.onProgress(reporter);
            }

            @Override
            public void addListener(Runnable listener, Executor executor) {
                transformed.addListener(listener, executor);
            }

            @Override
            public boolean cancel(boolean b) {
                return transformed.cancel(b);
            }

            @Override
            public boolean isCancelled() {
                return transformed.isCancelled();
            }

            @Override
            public boolean isDone() {
                return transformed.isDone();
            }

            @Override
            public U get() throws InterruptedException, ExecutionException {
                return transformed.get();
            }

            @Override
            public U get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
                return transformed.get(l, timeUnit);
            }
        };
    }
}
