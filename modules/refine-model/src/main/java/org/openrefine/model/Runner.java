
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
import org.openrefine.util.CloseableIterable;
import org.openrefine.util.CloseableIterator;

/**
 * Encapsulates the context required to implement, read and execute operations on {@link Grid} and {@link ChangeData}
 * objects.
 * 
 * Implementations should have a constructor with a single parameter, a {@link RunnerConfiguration} object, which is
 * used to initialize the runner.
 */
public interface Runner {

    // charset used when writing a grid to a file, in our internal format
    public static final Charset GRID_ENCODING = Charset.forName("UTF-8");
    // name of the empty file considered a completion marker in the serialization of Grid and ChangeData objects
    public static final String COMPLETION_MARKER_FILE_NAME = "_SUCCESS";

    /**
     * Loads a {@link Grid} serialized at a given location.
     * 
     * @param path
     *            the directory where the Grid is stored
     * @return the grid
     * @throws IOException
     *             when loading the grid failed, or when the grid's serialization was incomplete (lacking a _SUCCESS
     *             marker)
     */
    Grid loadGrid(File path) throws IOException;

    /**
     * Loads a {@link ChangeData} serialized at a given location.
     * 
     * @param path
     *            the directory where the ChangeData is stored
     * @param frozen
     *            assume that no other process is writing to the same disk location and therefore
     *            that iterating from this object should never wait for further writes even if
     *            the it is not complete
     * @throws IOException
     *             when loading the grid failed
     */
    <T> ChangeData<T> loadChangeData(File path, ChangeDataSerializer<T> serializer, boolean frozen) throws IOException;

    /**
     * Creates a {@link Grid} from an in-memory list of rows, which will be numbered from 0 to length-1.
     */
    Grid gridFromList(ColumnModel columnModel, List<Row> rows, Map<String, OverlayModel> overlayModels);

    /**
     * Creates a {@link Grid} from an iterable collection of rows. By default, this just gathers the iterable in a list
     * and delegates to {@link #gridFromList(ColumnModel, List, Map)}, but implementations may implement a different
     * approach which delays the loading of the collection in memory.
     *
     * @param rowCount
     *            if the number of rows is known, supply it in this parameter as it might improve efficiency. Otherwise,
     *            set to -1.
     * @param recordCount
     *            if the number of records is known, supply it in this parameter as it might improve efficiency.
     *            Otherwise, set to -1.
     */
    default Grid gridFromIterable(ColumnModel columnModel, CloseableIterable<Row> rows, Map<String, OverlayModel> overlayModels,
            long rowCount, long recordCount) {
        try (CloseableIterator<Row> iterator = rows.iterator()) {
            return gridFromList(columnModel, iterator.toJavaList(), overlayModels);
        }
    }

    /**
     * Loads a text file as a {@link Grid} with a single column named "Column" and whose contents are the lines in the
     * file, parsed as strings.
     * 
     * @param encoding
     *            TODO
     */
    Grid loadTextFile(String path, MultiFileReadingProgress progress, Charset encoding) throws IOException;

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
    Grid loadTextFile(String path, MultiFileReadingProgress progress, Charset encoding, long limit) throws IOException;

    /**
     * Creates a {@link ChangeData} from an in-memory list of indexed data. The list is required to be sorted.
     */
    <T> ChangeData<T> changeDataFromList(List<IndexedData<T>> changeData);

    /**
     * Creates a {@link ChangeData} from an iterable. By default, this just gathers the iterable in a list and delegates
     * to {@link #changeDataFromList(List)}, but implementations may implement a different approach which delays the
     * loading of the collection in memory.
     * 
     * @param itemCount
     *            if the number of items is known, supply it here, otherwise set this parameter to -1.
     */
    default <T> ChangeData<T> changeDataFromIterable(CloseableIterable<IndexedData<T>> iterable, long itemCount) {
        try (CloseableIterator<IndexedData<T>> iterator = iterable.iterator()) {
            return changeDataFromList(iterator.toJavaList());
        }
    }

    /**
     * Creates an empty change data object of a given type, marked as incomplete.
     */
    <T> ChangeData<T> emptyChangeData();

    /**
     * Indicates whether this implementation supports progress reporting. If not, progress objects will be left
     * untouched when passed to methods in this interface.
     */
    boolean supportsProgressReporting();
}
