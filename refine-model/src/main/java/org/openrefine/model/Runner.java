
package org.openrefine.model;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.openrefine.importers.MultiFileReadingProgress;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.overlay.OverlayModel;

/**
 * Encapsulates the context required to implement, read and execute operations on {@link Grid} objects.
 * 
 * Implementations should have a constructor with a single parameter, a {@link RunnerConfiguration} object, which is
 * used to initialize the runner.
 */
public interface Runner {

    // charset used when writing a grid to a file, in our internal format
    public static final Charset GRID_ENCODING = Charset.forName("UTF-8");

    /**
     * Loads a {@link Grid} serialized at a given location.
     * 
     * @param path
     *            the directory where the Grid is stored
     * @return the grid
     * @throws IOException
     *             when loading the grid failed
     */
    public Grid loadGrid(File path) throws IOException;

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
     * Creates a {@link Grid} from an in-memory list of rows, which will be numbered from 0 to length-1.
     */
    public Grid create(ColumnModel columnModel, List<Row> rows, Map<String, OverlayModel> overlayModels);

    /**
     * Loads a text file as a {@link Grid} with a single column named "Column" and whose contents are the lines in the
     * file, parsed as strings.
     * 
     * @param encoding
     *            TODO
     */
    public Grid loadTextFile(String path, MultiFileReadingProgress progress, Charset encoding) throws IOException;

    /**
     * Loads a text file as a {@link Grid} with a single column named "Column" and whose contents are the lines in the
     * file, parsed as strings.
     * 
     * @param path
     *            the path to the text file to load
     * @param encoding
     *            TODO
     * @param limit
     *            the maximum number of lines to read
     */
    public Grid loadTextFile(String path, MultiFileReadingProgress progress, Charset encoding, long limit) throws IOException;

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
