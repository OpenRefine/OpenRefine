
package org.openrefine.model;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.openrefine.importers.MultiFileReadingProgress;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.overlay.OverlayModel;

/**
 * Encapsulates the context required to implement, read and execute operations on {@link GridState} objects.
 * 
 * Implementations should have a constructor with a single parameter, a {@link RunnerConfiguration} object, which is
 * used to initialize the datamodel runner.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface DatamodelRunner {

    /**
     * Loads a {@link GridState} serialized at a given location.
     * 
     * @param path
     *            the directory where the GridState is stored
     * @return the grid
     * @throws IOException
     *             when loading the grid failed
     */
    public GridState loadGridState(File path) throws IOException;

    /**
     * Loads a {@link ChangeData} serialized at a given location.
     * 
     * @param path
     *            the directory where the ChangeData is stored
     * @throws IOException
     *             when loading the grid failed
     */
    public <T> ChangeData<T> loadChangeData(File path, ChangeDataSerializer<T> serializer) throws IOException;

    /**
     * Creates a {@link GridState} from an in-memory list of rows, which will be numbered from 0 to length-1.
     */
    public GridState create(ColumnModel columnModel, List<Row> rows, Map<String, OverlayModel> overlayModels);

    /**
     * Loads a text file as a {@link GridState} with a single column named "Column" and whose contents are the lines in
     * the file, parsed as strings.
     */
    public GridState loadTextFile(String path, MultiFileReadingProgress progress) throws IOException;

    /**
     * Loads a text file as a {@link GridState} with a single column named "Column" and whose contents are the lines in
     * the file, parsed as strings.
     * 
     * @param path
     *            the path to the text file to load
     * @param limit
     *            the maximum number of lines to read
     */
    public GridState loadTextFile(String path, MultiFileReadingProgress progress, long limit) throws IOException;

    /**
     * Creates a {@link ChangeData} from an in-memory list of indexed data. The list is required to be sorted.
     */
    public <T> ChangeData<T> create(List<IndexedData<T>> changeData);

    /**
     * Indicates whether this implementation supports progress reporting. If not, progress objects will be left
     * untouched when passed to methods in this interface.
     */
    public boolean supportsProgressReporting();
}
