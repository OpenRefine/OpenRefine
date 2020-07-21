package org.openrefine.model.changes;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;

import org.openrefine.model.DatamodelRunner;

/**
 * Some external data, obtained by communicating with
 * an external service or performing an expensive computation
 * whose result should be persisted in the project data.
 * 
 * This data is indexed by row ids from the original grid it 
 * was computed from, making it easy to join both to obtain
 * the final state of the grid after this expensive computation.
 * 
 * @author Antonin Delpeuch
 *
 * @param <T> the type of data to store for each row. It should be
 * serializable with Jackson.
 */
public interface ChangeData<T extends Serializable> extends Iterable<IndexedData<T>> {

    /**
     * Returns the change data at a given row.
     * 
     * @param rowId
     * @return null if there is no such change data for the given row id
     */
    public T get(long rowId);
    
    /**
     * The datamodel runner which was used to create
     * this change data.
     */
    public DatamodelRunner getDatamodelRunner();
    
    /**
     * Saves the change data to a specified directory,
     * following OpenRefine's format for change data.
     * 
     * @param file the directory where to save the grid state
     * @throws IOException
     */
    public void saveToFile(File file) throws IOException;
    
}
