
package org.openrefine.process;

import java.util.concurrent.Future;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Represents an asynchronous task which returns a result. In addition to the methods offered by {@link Future}, this
 * interface adds the ability to pause and resume the underlying process, as well as retrieve its progress, expressed as
 * a percentage.
 * 
 * @param <T>
 *            the type of value to be computed.
 */
public interface ProgressingFuture<T> extends ListenableFuture<T> {

    /**
     * Pauses the process that is computing the value to be returned in this future. If this process is not running,
     * this action has no effect.
     */
    void pause();

    /**
     * Resumes the process that is computing the value to be returned in this future. If this process had not been
     * paused before, this action has no effect.
     */
    void resume();

    /**
     * @return whether the process is currently paused
     */
    boolean isPaused();

    /**
     * @return whether the underlying process supports reporting progress beyond a boolean status, that is to say
     *         whether it can be in progress states strictly between 0 and 100.
     */
    boolean supportsProgress();

    /**
     * @return the current progress percentage of the task, between 0 and 100. If the task does not support reporting
     *         progress, this returns 0 until the task is successful, after which it returns 100. If the task results in
     *         an error, the progress might not reach 100.
     */
    int getProgress();

    /**
     * Registers a callback to be called every time the progress of this task changes. The callback will be called at
     * least once, on registration, with the current value of the progress.
     *
     * @param reporter
     *            the callback to call.
     */
    void onProgress(ProgressReporter reporter);
}
