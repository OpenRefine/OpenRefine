
package org.openrefine.process;

/**
 * Callback to report the progress of some long-running operation to the user. This is passed to the runner which
 * periodically calls `reportProgress` with the current percentage of completion of the task.
 */
public interface ProgressReporter {

    /**
     * Reports progress of the current long-running operation.
     * 
     * @param percentage
     *            a number from 0 to 100
     */
    public void reportProgress(int percentage);
}
