package org.openrefine.model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.openrefine.browsing.facets.RecordAggregator;
import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.RowChangeDataFlatJoiner;
import org.openrefine.model.changes.RowChangeDataJoiner;
import org.openrefine.model.changes.RowChangeDataProducer;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.sorting.SortingConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable object which represents the state of the project grid
 * at a given point in a workflow.
 */
public interface GridState {
    
    final static public String METADATA_PATH = "metadata.json";
    final static public String GRID_PATH = "grid";
    
    /**
     * @return the runner which created this grid state
     */
    public DatamodelRunner getDatamodelRunner();
    
    /**
     * @return the column metadata at this stage of the workflow
     */
    @JsonProperty("columnModel")
    public ColumnModel getColumnModel();
    
    /**
     * @param newColumnModel the column model to apply to the grid
     * @return a copy of this grid state with a modified column model.
     */
    public GridState withColumnModel(ColumnModel newColumnModel);
 
    /**
     * Returns a row by index. Repeatedly calling this method to obtain multiple rows
     * might be inefficient compared to fetching them by batch, depending on the implementation.
     * 
     * @param id the row index
     * @return the row at the given index
     * @throws IndexOutOfBoundsException if row id could not be found
     */
    public Row getRow(long id);
    
    /**
     * Returns a list of rows, starting from a given index and defined by a maximum
     * size.
     * 
     * @param start the first row id to fetch (inclusive)
     * @param limit the maximum number of rows to fetch
     * @return the list of rows with their ids (if any)
     */
    public List<IndexedRow> getRows(long start, int limit);
    
    /**
     * Among the subset of filtered rows, return a list of rows,
     * starting from a given index and defined by a maximum size.
     * 
     * @param filter the subset of rows to paginate through.
     *               This object and its dependencies are required
     *               to be serializable.
     * @param sortingConfig TODO
     * @param start the first row id to fetch (inclusive)
     * @param limit the maximum number of rows to fetch
     * @return the list of rows with their ids (if any)
     */
    public List<IndexedRow> getRows(RowFilter filter, SortingConfig sortingConfig, long start, int limit);
    
    /**
     * Iterate over rows matched by a filter. This might not require
     * loading all rows in memory at once, but might be less efficient
     * than {@link collectRows()} if all rows are to be stored in memory
     * downstream.
     */
    public Iterable<IndexedRow> iterateRows(RowFilter filter);
    
    /**
     * Count the number of rows which match a given filter.
     * 
     * @param filter the row filter
     * @return the number of rows for which this filter returns true
     */
    public long countMatchingRows(RowFilter filter);
    
    /**
     * Returns all rows in a list. This is inefficient
     * for large datasets as it forces the entire grid to be loaded in 
     * memory.
     */
    public List<IndexedRow> collectRows();

    /**
     * Returns a record obtained by its id. Repeatedly calling this method to obtain
     * multiple records might be inefficient depending on the implementation.
     * 
     * @param id the row id of the first row in the record
     * @return the corresponding record
     * @throws IllegalArgumentException if record id could not be found
     */
    public Record getRecord(long id);
    
    /**
     * Returns a list of records, starting from a given index and defined by a maximum
     * size.
     * 
     * @param start the first record id to fetch (inclusive)
     * @param limit the maximum number of records to fetch
     * @return the list of records (if any)
     */
    public List<Record> getRecords(long start, int limit);
 
    /**
     * Among the filtered subset of records, returns a list of records,
     * starting from a given index and defined by a maximum size.
     * 
     * @param filter the filter which defines the subset of records to paginate through
     *               This object and its dependencies are required
     *               to be serializable.
     * @param sortingConfig TODO
     * @param start the first record id to fetch (inclusive)
     * @param limit the maximum number of records to fetch
     * @return the list of records (if any)
     */
    public List<Record> getRecords(RecordFilter filter, SortingConfig sortingConfig, long start, int limit);
    
    /**
     * Iterate over records matched by a filter. This might not require
     * loading all records in memory at once, but might be less efficient
     * than {@link collectRecords()} if all records are to be stored in memory
     * downstream.
     */
    public Iterable<Record> iterateRecords(RecordFilter filter);

    /**
     * Return the number of records which are filtered by this filter.
     * 
     * @param filter the filter to evaluate
     * @return the number of records for which this filter evaluates to true
     */
    public long countMatchingRecords(RecordFilter filter);
    
    /**
     * Returns all records in a list. This is inefficient for large datasets
     * as it forces all records to be loaded in memory.
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
     * Saves the grid state to a specified directory,
     * following OpenRefine's format for grid storage.
     * 
     * @param file the directory where to save the grid state
     * @throws IOException
     */
    public void saveToFile(File file) throws IOException;
    
    // Aggregations

    /**
     * Computes the result of a row aggregator on the grid.
     */
    public <T extends Serializable> T aggregateRows(RowAggregator<T> aggregator, T initialState);
    
    /**
     * Computes the result of a row aggregator on the grid.
     */
    public <T extends Serializable> T aggregateRecords(RecordAggregator<T> aggregator, T initialState);

    // Transformations
    
    /**
     * Returns a new grid state where the overlay models have changed.
     * @param overlayModel the new overlay models to apply to the grid state
     * @return the changed grid state
     */
    public GridState withOverlayModels(Map<String, OverlayModel> overlayModel);
    
    /**
     * Returns a new grid state, where the rows have been mapped by the mapper.
     * 
     * @param mapper the function used to transform rows
     *               This object and its dependencies are required
     *               to be serializable.
     * @param newColumnModel the column model of the resulting grid state
     * @return the resulting grid state
     */
    public GridState mapRows(RowMapper mapper, ColumnModel newColumnModel);
    
    /**
     * Returns a new grid state, where the rows have been mapped by the flat mapper.
     * 
     * @param mapper the function used to transform rows
     *               This object and its dependencies are required
     *               to be serializable.
     * @param newColumnModel the column model of the resulting grid state
     * @return the resulting grid state
     */
    public GridState flatMapRows(RowFlatMapper mapper, ColumnModel newColumnModel);
    
    /**
     * Returns a new grid state where the rows have been mapped by the
     * stateful mapper. This can be significantly less efficient than a
     * stateless mapper, so only use this if you really need to rely on state.
     * 
     * @param <S> the type of state kept by the mapper
     * @param mapper the mapper to apply to the grid
     * @param newColumnModel the column model to apply to the new grid
     * @return
     */
    public <S extends Serializable> GridState mapRows(RowScanMapper<S> mapper, ColumnModel newColumnModel);
    
    /**
     * Returns a new grid state, where the records have been mapped by the mapper
     * 
     * @param filter the subset of records to which the mapper should be applied.
     *               This object and its dependencies are required
     *               to be serializable.
     * @param mapper the function used to transform records
     *               This object and its dependencies are required
     *               to be serializable.
     * @param newColumnModel the column model of the resulting grid state
     * @return the resulting grid state
     */
    public GridState mapRecords(RecordMapper mapper, ColumnModel newColumnModel);
    
    /**
     * Returns a new grid state where rows have been reordered according
     * to the configuration supplied.
     * 
     * @param sortingConfig the criteria to sort rows
     * @return the resulting grid state
     */
    public GridState reorderRows(SortingConfig sortingConfig);
    
    /**
     * Returns a new grid state where records have been reordered according
     * to the configuration supplied.
     * 
     * @param sortingConfig the criteria to sort records
     * @return the resulting grid state
     */
    public GridState reorderRecords(SortingConfig sortingConfig);
    
    /**
     * Removes all rows selected by a filter
     * 
     * @param filter which returns true when we should delete the row
     * @return the grid where the matching rows have been removed
     */
    public GridState removeRows(RowFilter filter);
    
    /**
     * Removes all records selected by a filter
     * 
     * @param filter which returns true when we should delete the record
     * @return the grid where the matching record have been removed
     */
    public GridState removeRecords(RecordFilter filter);
    
    // Interaction with change data
    
    /**
     * Extract change data by applying a function to each filtered row.
     * The calls to the change data producer are batched if requested by the producer.
     * 
     * @param <T> the type of change data that is serialized to disk for each row
     * @param filter a filter to select which rows to map
     * @param rowMapper produces the change data for each row
     * @return
     * @throws IllegalStateException if the row mapper returns a batch of results with a
     * different size than the batch of rows it was called on
     */
    public <T extends Serializable> ChangeData<T> mapRows(RowFilter filter, RowChangeDataProducer<T> rowMapper);
    
    /**
     * Joins pre-computed change data with the current grid data.
     * 
     * @param <T> the type of change data that was serialized to disk for each row
     * @param changeData the serialized change data
     * @param rowJoiner produces the new row by joining the old row with change data
     * @param newColumnModel the column model to apply to the new grid
     * @return
     */
    public <T extends Serializable> GridState join(ChangeData<T> changeData, RowChangeDataJoiner<T> rowJoiner, ColumnModel newColumnModel);
    
    /**
     * Joins pre-computed change data with the current grid data,
     * with a joiner function that can return multiple rows for a given original row.
     * 
     * @param <T> the type of change data that was serialized to disk for each row
     * @param changeData the serialized change data
     * @param rowJoiner produces the new row by joining the old row with change data
     * @param newColumnModel the column model to apply to the new grid
     * @return
     */
    public <T extends Serializable> GridState join(ChangeData<T> changeData, RowChangeDataFlatJoiner<T> rowJoiner, ColumnModel newColumnModel);
    
    /**
     * Utility class to help with deserialization of the metadata
     * without other attributes (such as number of rows)
     */
    public static class Metadata {
        @JsonProperty("columnModel")
        protected ColumnModel columnModel;
        @JsonProperty("overlayModels")
        Map<String, OverlayModel> overlayModels;
        @JsonProperty("rowCount")
        long rowCount = -1;
        @JsonProperty("recordCount")
        long recordCount = -1;
    }

}
