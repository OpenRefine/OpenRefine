
package org.openrefine.model;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.fs.FileSystem;

import org.openrefine.overlay.OverlayModel;

/**
 * Encapsulates the context required to implement, read and execute operations on {@link GridState} objects.
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
     * Returns a file system used by the implementation to read files to import.
     * 
     * @throws IOException
     */
    public FileSystem getFileSystem() throws IOException;

    /**
     * Creates a {@link GridState} from an in-memory list of rows, which will be numbered from 0 to length-1.
     */
    public GridState create(ColumnModel columnModel, List<Row> rows, Map<String, OverlayModel> overlayModels);

}
