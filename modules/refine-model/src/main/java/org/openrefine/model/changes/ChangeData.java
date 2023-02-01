
package org.openrefine.model.changes;

import java.io.File;
import java.io.IOException;

import org.openrefine.model.Runner;
import org.openrefine.process.ProgressReporter;

/**
 * Some external data, obtained by communicating with an external service or performing an expensive computation whose
 * result should be persisted in the project data.
 * <p>
 * This data is indexed by row ids from the original grid it was computed from, making it easy to join both to obtain
 * the final state of the grid after this expensive computation.
 *
 * @param <T>
 *            the type of data to store for each row. It should be serializable with Jackson.
 */
public interface ChangeData<T> extends Iterable<IndexedData<T>> {

    /**
     * Returns the change data at a given row.
     * 
     * @param rowId
     *            the 0-based row index
     * @return null if there is no such change data for the given row id
     */
    T get(long rowId);

    /**
     * The runner which was used to create this change data.
     */
    Runner getRunner();

    /**
     * Saves the change data to a specified directory, following OpenRefine's format for change data.
     * 
     * @param file
     *            the directory where to save the grid
     * @param serializer
     *            the serializer used to convert the items to strings
     */
    void saveToFile(File file, ChangeDataSerializer<T> serializer) throws IOException, InterruptedException;

    /**
     * Saves the change data to a specified directory, following OpenRefine's format for change data.
     * 
     * @param file
     *            the directory where to save the grid
     * @param serializer
     *            the serializer used to convert the items to strings
     * @param progressReporter
     *            callback to report the progress of the writing process
     */
    void saveToFile(File file, ChangeDataSerializer<T> serializer, ProgressReporter progressReporter)
            throws IOException, InterruptedException;

    /**
     * Whether the entire change data is available to be iterated on statically, without performing any new computation
     * or fetching. This happens when this ChangeData object is loaded back from disk and a suitable completion marker
     * was found.
     */
    boolean isComplete();

}
