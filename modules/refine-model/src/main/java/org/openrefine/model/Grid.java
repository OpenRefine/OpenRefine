
package org.openrefine.model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

import org.openrefine.browsing.facets.RecordAggregator;
import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.RecordChangeDataJoiner;
import org.openrefine.model.changes.RecordChangeDataProducer;
import org.openrefine.model.changes.RowChangeDataFlatJoiner;
import org.openrefine.model.changes.RowChangeDataJoiner;
import org.openrefine.model.changes.RowChangeDataProducer;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.overlay.OverlayModelResolver;
import org.openrefine.process.ProgressReporter;
import org.openrefine.sorting.SortingConfig;

/**
 * Immutable object which represents the state of the project grid at a given point in a workflow.
 */
public interface Grid {

    final static public String METADATA_PATH = "metadata.json";
    final static public String GRID_PATH = "grid";

    /**
     * @return the runner which created this grid
     */
    public Runner getDatamodelRunner();

    /**
     * @return the column metadata at this stage of the workflow
     */
    @JsonProperty("columnModel")
    public ColumnModel getColumnModel();

    /**
     * @param newColumnModel
     *            the column model to apply to the grid
     * @return a copy of this grid with a modified column model.
     */
    public Grid withColumnModel(ColumnModel newColumnModel);

    /**
     * Returns a row by index. Repeatedly calling this method to obtain multiple rows might be inefficient compared to
     * fetching them by batch, depending on the implementation.
     * 
     * @param id
     *            the row index. This refers to the current position of the row in the grid, which corresponds to
     *            {@link IndexedRow#getIndex()}.
     * @return the row at the given index
     * @throws IndexOutOfBoundsException
     *             if row id could not be found
     */
    public Row getRow(long id);

    /**
     * Returns a list of rows, starting from a given index and defined by a maximum size.
     * 
     * @param start
     *            the first row id to fetch (inclusive)
     * @param limit
     *            the maximum number of rows to fetch
     * @return the list of rows with their ids (if any)
     */
    public List<IndexedRow> getRowsAfter(long start, int limit);

    /**
     * Among the subset of filtered rows, return a list of rows, starting from a given index and defined by a maximum
     * size.
     * 
     * @param filter
     *            the subset of rows to paginate through. This object and its dependencies are required to be
     *            serializable.
     * @param start
     *            the first row id to fetch (inclusive)
     * @param limit
     *            the maximum number of rows to fetch
     * @return the list of rows with their ids (if any)
     * @see #getRowsBefore(long, int)
     */
    public List<IndexedRow> getRowsAfter(RowFilter filter, long start, int limit);

    /**
     * Returns a list of consecutive rows, just before the given row index (not included) and up to a maximum size.
     *
     * @param end
     *            the last row id to fetch (exclusive)
     * @param limit
     *            the maximum number of rows to fetch
     * @return the list of rows with their ids (if any)
     * @see #getRowsAfter(long, int)
     */
    public List<IndexedRow> getRowsBefore(long end, int limit);

    /**
     * Among the subset of filtered rows, return a list of rows, just before the row with a given index (excluded) and
     * defined by a maximum size.
     *
     * @param filter
     *            the subset of rows to paginate through. This object and its dependencies are required to be
     *            serializable.
     * @param end
     *            the last row id to fetch (exclusive)
     * @param limit
     *            the maximum number of rows to fetch
     * @return the list of rows with their ids (if any)
     */
    public List<IndexedRow> getRowsBefore(RowFilter filter, long end, int limit);

    /**
     * Returns a list of rows corresponding to the row indices supplied. By default, this calls
     * {@link Grid#getRow(long)} on all values, but implementations can override this to more efficient strategies if
     * available.
     *
     * @param rowIndices
     *            the indices of the rows to lookup
     * @return the list contains null values for the row indices which could not be found.
     */
    public default List<IndexedRow> getRows(List<Long> rowIndices) {
        List<IndexedRow> result = new ArrayList<>(rowIndices.size());
        for (long rowId : rowIndices) {
            try {
                result.add(new IndexedRow(rowId, getRow(rowId)));
            } catch (IndexOutOfBoundsException e) {
                result.add(null);
            }
        }
        return result;
    }

    /**
     * Iterate over rows matched by a filter, in the order determined by a sorting configuration. This might not require
     * loading all rows in memory at once, but might be less efficient than {@link #collectRows()} if all rows are to be
     * stored in memory downstream.
     * <p>
     * TODO users of this method might actually be required to iterate up to the end of the iterator to avoid resource
     * leaks with some implementations. This should be clarified by the interface. Consider exposing a closeable
     * iterable instead.
     */
    public Iterable<IndexedRow> iterateRows(RowFilter filter);

    /**
     * Count the number of rows which match a given filter.
     * 
     * @param filter
     *            the row filter
     * @return the number of rows for which this filter returns true
     */
    public long countMatchingRows(RowFilter filter);

    /**
     * Return the number of rows matching the given row filter, but by processing at most a fixed number of row.
     * 
     * @param filter
     *            counts the number of records on which it returns true
     * @param limit
     *            maximum number of records to process
     */
    public ApproxCount countMatchingRowsApprox(RowFilter filter, long limit);

    /**
     * Returns all rows in a list. This is inefficient for large datasets as it forces the entire grid to be loaded in
     * memory.
     */
    public List<IndexedRow> collectRows();

    /**
     * Returns a record obtained by its id. Repeatedly calling this method to obtain multiple records might be
     * inefficient depending on the implementation.
     * 
     * @param id
     *            the row id of the first row in the record. This refers to the current position of the record in the
     *            grid, which corresponds to {@link Record#getStartRowId()}.
     * @return the corresponding record
     * @throws IllegalArgumentException
     *             if record id could not be found
     */
    public Record getRecord(long id);

    /**
     * Returns a list of records, starting from a given index and defined by a maximum size.
     * 
     * @param start
     *            the first record id to fetch (inclusive)
     * @param limit
     *            the maximum number of records to fetch
     * @return the list of records (if any)
     * @see #getRecordsBefore(long, int)
     */
    public List<Record> getRecordsAfter(long start, int limit);

    /**
     * Among the filtered subset of records, returns a list of records, starting from a given index and defined by a
     * maximum size.
     * 
     * @param filter
     *            the filter which defines the subset of records to paginate through This object and its dependencies
     *            are required to be serializable.
     * @param start
     *            the first record id to fetch (inclusive)
     * @param limit
     *            the maximum number of records to fetch
     * @return the list of records (if any)
     */
    public List<Record> getRecordsAfter(RecordFilter filter, long start, int limit);

    /**
     * Returns a list of consecutive records, ending at a given index (exclusive) and defined by a maximum size.
     *
     * @param end
     *            the last record id to fetch (exclusive)
     * @param limit
     *            the maximum number of records to fetch
     * @return the list of records (if any)
     * @see #getRecordsAfter(long, int)
     */
    public List<Record> getRecordsBefore(long end, int limit);

    /**
     * Among the filtered subset of records, returns a list of records, ending at a given index (exclusive) and defined
     * by a maximum size.
     *
     * @param filter
     *            the filter which defines the subset of records to paginate through This object and its dependencies
     *            are required to be serializable.
     * @param end
     *            the last record id to fetch (exclusive)
     * @param limit
     *            the maximum number of records to fetch
     * @return the list of records (if any)
     */
    public List<Record> getRecordsBefore(RecordFilter filter, long end, int limit);

    /**
     * Iterate over records matched by a filter. This might not require loading all records in memory at once, but might
     * be less efficient than {@link #collectRecords()} if all records are to be stored in memory downstream.
     */
    public Iterable<Record> iterateRecords(RecordFilter filter);

    /**
     * Return the number of records which are filtered by this filter.
     * 
     * @param filter
     *            the filter to evaluate
     * @return the number of records for which this filter evaluates to true
     */
    public long countMatchingRecords(RecordFilter filter);

    /**
     * Return the number of records matching the given record filter, but by processing at most a fixed number of
     * records.
     * 
     * @param filter
     *            counts the number of records on which it returns true
     * @param limit
     *            maximum number of records to process
     */
    public ApproxCount countMatchingRecordsApprox(RecordFilter filter, long limit);

    /**
     * Returns all records in a list. This is inefficient for large datasets as it forces all records to be loaded in
     * memory.
     */
    public List<Record> collectRecords();

    /**
     * @return the number of rows in the table
     */
    @JsonProperty("rowCount")
    public long rowCount();

    /**
     * @return the number of records in the table
     */
    @JsonProperty("recordCount")
    public long recordCount();

    /**
     * @return the overlay models in this state
     */
    @JsonProperty("overlayModels")
    public Map<String, OverlayModel> getOverlayModels();

    /**
     * Saves the grid to a specified directory, following OpenRefine's format for grid storage.
     * 
     * @param file
     *            the directory where to save the grid
     */
    public void saveToFile(File file) throws IOException;

    /**
     * Saves the grid to a specified directory, following OpenRefine's format for grid storage.
     * 
     * @param file
     *            the directory where to save the grid
     * @param progressReporter
     *            reports the progress of the writing process
     */
    public void saveToFile(File file, ProgressReporter progressReporter) throws IOException, InterruptedException;

    // Aggregations

    /**
     * Computes the result of a row aggregator on the grid.
     */
    public <T extends Serializable> T aggregateRows(RowAggregator<T> aggregator, T initialState);

    /**
     * Computes the result of a row aggregator on the grid.
     */
    public <T extends Serializable> T aggregateRecords(RecordAggregator<T> aggregator, T initialState);

    /**
     * Computes the result of a row aggregator on the grid, reading at most a fixed number of rows. The rows read should
     * be deterministic for a given implementation.
     */
    public <T extends Serializable> PartialAggregation<T> aggregateRowsApprox(RowAggregator<T> aggregator, T initialState, long maxRows);

    /**
     * Computes the result of a row aggregator on the grid, reading at most a fixed number of records. The records read
     * should be deterministic for a given implementation.
     */
    public <T extends Serializable> PartialAggregation<T> aggregateRecordsApprox(RecordAggregator<T> aggregator, T initialState,
            long maxRecords);

    // Transformations

    /**
     * Returns a new grid where the overlay models have changed.
     * 
     * @param overlayModel
     *            the new overlay models to apply to the grid
     * @return the changed grid
     */
    public Grid withOverlayModels(Map<String, OverlayModel> overlayModel);

    /**
     * Returns a new grid, where the rows have been mapped by the mapper.
     * 
     * @param mapper
     *            the function used to transform rows This object and its dependencies are required to be serializable.
     * @param newColumnModel
     *            the column model of the resulting grid
     * @return the resulting grid
     */
    public Grid mapRows(RowMapper mapper, ColumnModel newColumnModel);

    /**
     * Returns a new grid, where the rows have been mapped by the flat mapper.
     * 
     * @param mapper
     *            the function used to transform rows This object and its dependencies are required to be serializable.
     * @param newColumnModel
     *            the column model of the resulting grid
     * @return the resulting grid
     */
    public Grid flatMapRows(RowFlatMapper mapper, ColumnModel newColumnModel);

    /**
     * Returns a new grid where the rows have been mapped by the stateful mapper. This can be significantly less
     * efficient than a stateless mapper, so only use this if you really need to rely on state.
     * 
     * @param <S>
     *            the type of state kept by the mapper
     * @param mapper
     *            the mapper to apply to the grid
     * @param newColumnModel
     *            the column model to apply to the new grid
     */
    public <S extends Serializable> Grid mapRows(RowScanMapper<S> mapper, ColumnModel newColumnModel);

    /**
     * Returns a new grid, where the records have been mapped by the mapper
     *
     * @param mapper
     *            the function used to transform records This object and its dependencies are required to be
     *            serializable.
     * @param newColumnModel
     *            the column model of the resulting grid
     * @return the resulting grid
     */
    public Grid mapRecords(RecordMapper mapper, ColumnModel newColumnModel);

    /**
     * Returns a new grid where rows have been reordered according to the configuration supplied.
     * 
     * @param sortingConfig
     *            the criteria to sort rows
     * @param permanent
     *            if true, forget the original row ids. If false, store them in the corresponding
     *            {@link IndexedRow#getOriginalIndex()}.
     * @return the resulting grid
     */
    public Grid reorderRows(SortingConfig sortingConfig, boolean permanent);

    /**
     * Returns a new grid where records have been reordered according to the configuration supplied.
     * 
     * @param sortingConfig
     *            the criteria to sort records
     * @param permanent
     *            if true, forget the original record ids. If false, store them in the corresponding
     *            {@link Record#getOriginalStartRowId()}.
     * @return the resulting grid
     */
    public Grid reorderRecords(SortingConfig sortingConfig, boolean permanent);

    /**
     * Removes all rows selected by a filter
     * 
     * @param filter
     *            which returns true when we should delete the row
     * @return the grid where the matching rows have been removed
     */
    public Grid removeRows(RowFilter filter);

    /**
     * Removes all records selected by a filter
     * 
     * @param filter
     *            which returns true when we should delete the record
     * @return the grid where the matching record have been removed
     */
    public Grid removeRecords(RecordFilter filter);

    /**
     * Only keep the first rows.
     * <p>
     * By default, this uses {@link #removeRows(RowFilter)} to remove the last rows, but implementations can override
     * this for efficiency.
     * 
     * @param rowLimit
     *            the number of rows to keep
     * @return the limited grid
     */
    public default Grid limitRows(long rowLimit) {
        return removeRows(RowFilter.limitFilter(rowLimit));
    }

    /**
     * Drop the first rows.
     * <p>
     * By default, this uses {@link #removeRows(RowFilter)} to remove the first rows, but implementations can override
     * this for efficiency.
     * 
     * @param rowsToDrop
     *            the number of rows to drop
     * @return the grid consisting of the last rows
     */
    public default Grid dropRows(long rowsToDrop) {
        return removeRows(RowFilter.dropFilter(rowsToDrop));
    }

    // Interaction with change data

    /**
     * Extract change data by applying a function to each filtered row. The calls to the change data producer are
     * batched if requested by the producer.
     * <p>
     * 
     * @param <T>
     *            the type of change data that is serialized to disk for each row
     * @param filter
     *            a filter to select which rows to map
     * @param rowMapper
     *            produces the change data for each row
     * @throws IllegalStateException
     *             if the row mapper returns a batch of results with a different size than the batch of rows it was
     *             called on
     */
    public <T> ChangeData<T> mapRows(RowFilter filter, RowChangeDataProducer<T> rowMapper);

    /**
     * Extract change data by applying a function to each filtered record. The calls to the change data producer are
     * batched if requested by the producer.
     * 
     * @param <T>
     *            the type of change data that is serialized to disk for each row
     * @param filter
     *            a filter to select which rows to map
     * @param recordMapper
     *            produces the change data for each record
     * @throws IllegalStateException
     *             if the record mapper returns a batch of results with a different size than the batch of records it
     *             was called on
     */
    public <T> ChangeData<T> mapRecords(RecordFilter filter, RecordChangeDataProducer<T> recordMapper);

    /**
     * Joins pre-computed change data with the current grid data, row by row.
     * 
     * @param <T>
     *            the type of change data that was serialized to disk for each row
     * @param changeData
     *            the serialized change data
     * @param rowJoiner
     *            produces the new row by joining the old row with change data
     * @param newColumnModel
     *            the column model to apply to the new grid
     */
    public <T> Grid join(ChangeData<T> changeData, RowChangeDataJoiner<T> rowJoiner, ColumnModel newColumnModel);

    /**
     * Joins pre-computed change data with the current grid data, with a joiner function that can return multiple rows
     * for a given original row.
     * 
     * @param <T>
     *            the type of change data that was serialized to disk for each row
     * @param changeData
     *            the serialized change data
     * @param rowJoiner
     *            produces the new row by joining the old row with change data
     * @param newColumnModel
     *            the column model to apply to the new grid
     */
    public <T> Grid join(ChangeData<T> changeData, RowChangeDataFlatJoiner<T> rowJoiner, ColumnModel newColumnModel);

    /**
     * Joins pre-computed change data with the current grid data, record by record.
     * 
     * @param <T>
     *            the type of change data that was serialized to disk for each record
     * @param changeData
     *            the serialized change data
     * @param recordJoiner
     *            produces the new list of rows by joining the old record with change data
     * @param newColumnModel
     *            the column model to apply to the new grid
     */
    public <T> Grid join(ChangeData<T> changeData, RecordChangeDataJoiner<T> recordJoiner, ColumnModel newColumnModel);

    // Union of grids

    /**
     * Creates a new grid containing all rows in this grid, followed by all rows in the other grid supplied. The overlay
     * models of this grid have priority over the others.
     * <p>
     * The two grids are required to have the same number of columns.
     * 
     * @param other
     *            the grid to concatenate to this one
     * @return a new grid, union of the two
     */
    public Grid concatenate(Grid other);

    // Memory management

    /**
     * Is this grid cached in memory? If not, its contents are stored on disk.
     */
    public boolean isCached();

    /**
     * Free up any memory used to cache this grid in memory.
     */
    public void uncache();

    /**
     * Attempt to cache this grid in memory. If the grid is too big, this can fail.
     * 
     * @return whether the grid was actually cached in memory.
     */
    public boolean cache();

    /**
     * Attempt to cache this grid in memory. If the grid is too big, this can fail.
     * 
     * @param progressReporter
     *            callback to report the progress of the storage of the values
     * @return whether the grid was actually cached in memory.
     */
    public boolean cache(ProgressReporter progressReporter);

    /**
     * Utility class to represent the outcome of a partial count: the number of records/rows processed, and how many of
     * these fulfilled the condition. The limitReached flag indicates whether the aggregation stopped because the limit
     * was reached or the underlying collection was exhausted.
     */
    public static class ApproxCount implements Serializable {

        private static final long serialVersionUID = -6472934740385946264L;
        private final long _processed;
        private final long _matched;
        private final boolean _limitReached;

        public ApproxCount(long processed, long matched, boolean limitReached) {
            _processed = processed;
            _matched = matched;
            _limitReached = limitReached;
        }

        public long getProcessed() {
            return _processed;
        }

        public long getMatched() {
            return _matched;
        }

        public boolean limitReached() {
            return _limitReached;
        }
    }

    /**
     * Utility class to represent the result of a partial aggregation
     */
    public static class PartialAggregation<T extends Serializable> implements Serializable {

        private static final long serialVersionUID = -3669547386094478153L;
        private final T _state;
        private final long _processed;
        private final boolean _limitReached;

        public PartialAggregation(T state, long processed, boolean limitReached) {
            _state = state;
            _processed = processed;
            _limitReached = limitReached;
        }

        public T getState() {
            return _state;
        }

        public long getProcessed() {
            return _processed;
        }

        public boolean limitReached() {
            return _limitReached;
        }
    }

    /**
     * Utility class to help with deserialization of the metadata without other attributes (such as number of rows)
     */
    public static class Metadata {

        @JsonProperty("columnModel")
        public ColumnModel columnModel;

        @JsonProperty("overlayModels")
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "overlayModelType", visible = true) // for
                                                                                                                                     // UnknownOverlayModel,
                                                                                                                                     // which
                                                                                                                                     // needs
                                                                                                                                     // to
                                                                                                                                     // read
                                                                                                                                     // its
                                                                                                                                     // own
                                                                                                                                     // id
        @JsonTypeIdResolver(OverlayModelResolver.class)
        public Map<String, OverlayModel> overlayModels;

        @JsonProperty("rowCount")
        public long rowCount = -1;

        @JsonProperty("recordCount")
        public long recordCount = -1;
    }

}
