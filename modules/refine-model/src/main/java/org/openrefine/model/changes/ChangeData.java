
package org.openrefine.model.changes;

import java.io.File;
import java.io.IOException;

import org.openrefine.model.Runner;
import org.openrefine.process.ProgressingFuture;
import org.openrefine.util.CloseableIterable;

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
public interface ChangeData<T> extends CloseableIterable<IndexedData<T>> {

    /**
     * A marker at the end of the serialization of a partition of a change data, signalling that the end of the
     * partition was reached. This is useful when iterating synchronously from a ChangeData object which is being
     * written simultaneously.
     */
    public static final String partitionEndMarker = "end";

    /**
     * Returns the change data at a given row. The data encapsulated in this {@link IndexedData} may be null, but not
     * the return value of this function itself.
     *
     * @param rowId
     *            the 0-based row index
     */
    IndexedData<T> get(long rowId);

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
     * Saves the change data to a specified directory, following OpenRefine's format for change data, in an asynchronous
     * way.
     *
     * @param file
     *            the directory where to save the grid
     * @param serializer
     *            the serializer used to convert the items to strings
     * @return a future which completes once the save is complete
     */
    ProgressingFuture<Void> saveToFileAsync(File file, ChangeDataSerializer<T> serializer);

    /**
     * Whether the entire change data is available to be iterated on statically, without performing any new computation
     * or fetching. This happens when this ChangeData object is loaded back from disk and a suitable completion marker
     * was found.
     */
    boolean isComplete();

}
