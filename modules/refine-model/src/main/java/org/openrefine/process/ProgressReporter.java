
package org.openrefine.process;

/**
 * Callback to report the progress of some long-running operation to the user. This is passed to the runner which
 * periodically calls `reportProgress` with the current percentage of completion of the task.
 */
public interface ProgressReporter {

    /**
     * Reports progress of the current long-running operation, with additional information about how many rows have been
     * processed, over a total number of rows/records to process. Both are set to 0 if the numbers of rows are unknown.
     * 
     * @param percentage
     *            a number from 0 to 100
     * @param processedElements
     *            the number of rows processed by the operation
     * @param totalElements
     *            the total number of rows to be processed by the operation
     */
    public void reportProgress(int percentage, long processedElements, long totalElements);

    /**
     * Convenience method to report progress without known row counts.
     * 
     * @param percentage
     *            a number from 0 to 100
     */
    public default void reportProgress(int percentage) {
        reportProgress(percentage, 0L, 0L);
    }
}
