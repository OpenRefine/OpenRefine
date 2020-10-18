package org.openrefine.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import org.openrefine.browsing.facets.RecordAggregator;
import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.RecordChangeDataJoiner;
import org.openrefine.model.changes.RecordChangeDataProducer;
import org.openrefine.model.changes.RowChangeDataFlatJoiner;
import org.openrefine.model.changes.RowChangeDataJoiner;
import org.openrefine.model.changes.RowChangeDataProducer;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.sorting.RecordSorter;
import org.openrefine.sorting.RowSorter;
import org.openrefine.sorting.SortingConfig;
import org.openrefine.util.ParsingUtilities;
import org.testng.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Iterators;

/**
 * A massively inefficient but very simple implementation of a GridState,
 * for testing purposes.
 * 
 * @author Antonin Delpeuch
 *
 */
public class TestingGridState implements GridState {
    
    private ColumnModel columnModel;
    private Map<String, OverlayModel> overlayModels;
    private List<Row> rows;
    private List<Record> records;
    
    public TestingGridState(ColumnModel columnModel, List<Row> rows, Map<String, OverlayModel> overlayModels) {
        this.columnModel = columnModel;
        this.rows = rows;
        this.overlayModels = overlayModels;
        records = groupRowsIntoRecords(rows, columnModel.getKeyColumnIndex());
    }
    
    public static List<Record> groupRowsIntoRecords(List<Row> rows, int keyCellIndex) {
        List<Record> records = new ArrayList<>();
        List<Row> currentRecord = new ArrayList<>();
        int recordStart = 0;
        for(int i = 0; i < rows.size(); i++) {
            if(Record.isRecordStart(rows.get(i), keyCellIndex) && !currentRecord.isEmpty()) {
                records.add(new Record(recordStart, currentRecord));
                recordStart = i;
                currentRecord = new ArrayList<>();
            }
            currentRecord.add(rows.get(i));
        }
        if(!currentRecord.isEmpty()) {
            records.add(new Record(recordStart, currentRecord));
        }
        return records;
    }

    @Override
    public ColumnModel getColumnModel() {
        return columnModel;
    }

    @Override
    public Row getRow(long id) {
        return rows.get((int) id);
    }
    
    private List<IndexedRow> indexedRows() {
        return IntStream.range(0, rows.size()).mapToObj(i ->  new IndexedRow((long)i, rows.get(i))).collect(Collectors.toList());
    }

    @Override
    public List<IndexedRow> getRows(long start, int limit) {
        return indexedRows().subList(
                Math.min((int)start, rows.size()),
                Math.min((int)start + limit, rows.size()));
    }
    
    @Override
    public List<IndexedRow> getRows(RowFilter filter, SortingConfig sortingConfig, long start, int limit) {
        // Check that the filter is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        RowFilter deserializedFilter = TestingDatamodelRunner.serializeAndDeserialize(filter);
        
        List<IndexedRow> sortedRows = sortedRows(sortingConfig);
        return sortedRows.stream()
                .filter(tuple -> deserializedFilter.filterRow(tuple.getIndex(), tuple.getRow()))
                .skip(start)
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<IndexedRow> collectRows() {
        return indexedRows();
    }

    @Override
    public Record getRecord(long id) {
        List<Record> matching = getRecords(id, 1);
        if (matching.isEmpty() || matching.get(0).getStartRowId() != id) {
            throw new IllegalArgumentException(String.format("No record with id %d", id));
        }
        return matching.get(0);
    }

    @Override
    public List<Record> getRecords(long start, int limit) {
        return records
                .stream()
                .filter(record -> record.getStartRowId() >= start)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Record> getRecords(RecordFilter filter, SortingConfig sortingConfig, long start, int limit) {
        // Check that the filter is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        RecordFilter deserializedFilter = TestingDatamodelRunner.serializeAndDeserialize(filter);
        List<Record> sorted = sortedRecords(sortingConfig);
        return sorted
                .stream()
                .filter(record -> record.getStartRowId() >= start && deserializedFilter.filterRecord(record))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Record> collectRecords() {
        return records;
    }

    @Override
    public long rowCount() {
        return rows.size();
    }

    @Override
    public long recordCount() {
        return records.size();
    }
    
    @Override
    public long countMatchingRows(RowFilter filter) {
        return indexedRows()
                .stream()
                .filter(tuple -> filter.filterRow(tuple.getIndex(), tuple.getRow()))
                .count();
    }

    @Override
    public long countMatchingRecords(RecordFilter filter) {
        return records
                .stream()
                .filter(record -> filter.filterRecord(record))
                .count();
    }


    @Override
    public Map<String, OverlayModel> getOverlayModels() {
        return overlayModels;
    }

    @Override
    public void saveToFile(File file) throws IOException {
        File gridPath = new File(file, GridState.GRID_PATH);
        File metadataPath = new File(file, GridState.METADATA_PATH);
        
        gridPath.mkdirs();
        File partFile = new File(gridPath, "part-00000.gz");
        FileOutputStream fos = null;
        GZIPOutputStream gos = null;
        try {
            fos = new FileOutputStream(partFile);
            gos = new GZIPOutputStream(fos);
            for(IndexedRow row : indexedRows()) {
                ParsingUtilities.mapper.writeValue(gos, row);
                gos.write('\n');
            }
        } finally {
            if (gos != null) {
                gos.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
        
        ParsingUtilities.saveWriter.writeValue(metadataPath, this);
    }

    @Override
    public <T extends Serializable> T aggregateRows(RowAggregator<T> aggregator, T initialState) {
          return aggregateRowsApprox(aggregator, initialState, rows.size());
    }
    
    @Override
    public <T extends Serializable> T aggregateRecords(RecordAggregator<T> aggregator, T initialState) {
        return aggregateRecordsApprox(aggregator, initialState, records.size());
    }
    
    @Override
    public <T extends Serializable> T aggregateRowsApprox(RowAggregator<T> aggregator, T initialState, long maxRows) {
        initialState = TestingDatamodelRunner.serializeAndDeserialize(initialState);
        aggregator = TestingDatamodelRunner.serializeAndDeserialize(aggregator);
        // Artificially split the grid in two, in order to use the `sum` method
        // of RowAggregator.
        long split = rowCount() / 2;
        T statesA = initialState;
        T statesB = initialState;
        long count = 0;
        for(IndexedRow row : indexedRows()) {
            if (count == maxRows) {
                break;
            }
            if (row.getIndex() < split) {
                statesA = aggregator.withRow(statesA, row.getIndex(), row.getRow());
            } else {
                statesB = aggregator.withRow(statesB, row.getIndex(), row.getRow());
            }
            count += 1;
        }
        
        return aggregator.sum(statesA, statesB);
    }

    @Override
    public <T extends Serializable> T aggregateRecordsApprox(RecordAggregator<T> aggregator, T initialState,
            long maxRecords) {
        initialState = TestingDatamodelRunner.serializeAndDeserialize(initialState);
        aggregator = TestingDatamodelRunner.serializeAndDeserialize(aggregator);
        // Artificially split the grid in two, in order to use the `sum` method
        // of FacetAggregator.
        long split = rowCount() / 2;
        T statesA = initialState;
        T statesB = initialState;
        long count = 0;
        for(Record record : records) {
            if (count == maxRecords) {
                break;
            }
            if (record.getStartRowId() < split) {
                statesA = aggregator.withRecord(statesA, record);
            } else {
                statesB = aggregator.withRecord(statesB, record);
            }
            count += 1;
        }
        
        return aggregator.sum(statesA, statesB);
    }

    @Override
    public GridState mapRows(RowMapper mapper, ColumnModel newColumnModel) {
        // Check that the mapper is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        mapper = TestingDatamodelRunner.serializeAndDeserialize(mapper);
        
        List<Row> rows = new ArrayList<>(this.rows.size());
        for(IndexedRow indexedRow : indexedRows()) {
            Row row = mapper.call(indexedRow.getIndex(), indexedRow.getRow());
            if (row.getCells().size() != newColumnModel.getColumns().size()) {
                Assert.fail(String.format("Row size (%d) inconsistent with supplied column model (%s)",
                        row.getCells().size(), newColumnModel.getColumns()));
            }
            rows.add(row);
        }
        return new TestingGridState(newColumnModel, rows, overlayModels);
    }
    

    @Override
    public GridState flatMapRows(RowFlatMapper mapper, ColumnModel newColumnModel) {
        // Check that the mapper is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        mapper = TestingDatamodelRunner.serializeAndDeserialize(mapper);
        
        List<Row> rows = new ArrayList<>(this.rows.size());
        for(IndexedRow indexedRow : indexedRows()) {
            List<Row> rowBatch = mapper.call(indexedRow.getIndex(), indexedRow.getRow());
            for (Row row : rowBatch) {
                if (row.getCells().size() != newColumnModel.getColumns().size()) {
                    Assert.fail(String.format("Row size (%d) inconsistent with supplied column model (%s)",
                            row.getCells().size(), newColumnModel.getColumns()));
                }
            }
            rows.addAll(rowBatch);
        }
        return new TestingGridState(newColumnModel, rows, overlayModels);
    }

    @Override
    public <S extends Serializable> GridState mapRows(RowScanMapper<S> mapper, ColumnModel newColumnModel) {
        // Check that the mapper is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        mapper = TestingDatamodelRunner.serializeAndDeserialize(mapper);
        
        S currentState = mapper.unit();
        List<Row> rows = new ArrayList<>(this.rows.size());
        for(IndexedRow indexedRow : indexedRows()) {
            Row row = mapper.map(currentState, indexedRow.getIndex(), indexedRow.getRow());
            currentState = mapper.combine(currentState, mapper.feed(indexedRow.getIndex(), indexedRow.getRow()));
            if (row.getCells().size() != newColumnModel.getColumns().size()) {
                Assert.fail(String.format("Row size (%d) inconsistent with supplied column model (%s)",
                        row.getCells().size(), newColumnModel.getColumns()));
            }
            rows.add(row);
        }
        return new TestingGridState(newColumnModel, rows, overlayModels);
    }


    @Override
    public GridState mapRecords(RecordMapper mapper, ColumnModel newColumnModel) {
        // Check that the mapper is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        mapper = TestingDatamodelRunner.serializeAndDeserialize(mapper);
        
        List<Row> rows = new ArrayList<>(this.rows.size());
        for(Record record : records) {
            List<Row> addedRows = mapper.call(record);
            for(Row row : addedRows) {
                if (row.getCells().size() != newColumnModel.getColumns().size()) {
                    Assert.fail("Row size inconsistent with supplied column model");
                }
            }
            rows.addAll(addedRows);
        }
        return new TestingGridState(newColumnModel, rows, overlayModels);
    }

    @Override
    public Iterable<IndexedRow> iterateRows(RowFilter filter, SortingConfig sortingConfig) {
    	List<IndexedRow> sortedRows = sortedRows(sortingConfig);
        return new Iterable<IndexedRow>() {

            @Override
            public Iterator<IndexedRow> iterator() {
                return sortedRows
                        .stream()
                        .filter(ir -> filter.filterRow(ir.getIndex(), ir.getRow()))
                        .iterator();
            }
            
        };
    }

    @Override
    public Iterable<Record> iterateRecords(RecordFilter filter, SortingConfig sortingConfig) {
    	List<Record> sorted = sortedRecords(sortingConfig);
        return new Iterable<Record>() {

            @Override
            public Iterator<Record> iterator() {
                return sorted
                        .stream()
                        .filter(r -> filter.filterRecord(r))
                        .iterator();
            }
            
        };
    }

    @Override
    public GridState withOverlayModels(Map<String, OverlayModel> overlayModel) {
        return new TestingGridState(columnModel, rows, overlayModel);
    }

    @Override
    public GridState withColumnModel(ColumnModel newColumnModel) {
        return new TestingGridState(newColumnModel, rows, overlayModels);
    }

    @Override
    public GridState reorderRows(SortingConfig sortingConfig) {
        return new TestingGridState(columnModel,
                sortedRows(sortingConfig).stream().map(r -> r.getRow()).collect(Collectors.toList()),
                overlayModels);
    }

    @Override
    public GridState reorderRecords(SortingConfig sortingConfig) {
        List<Row> newRows = new ArrayList<>(rows.size());
        if (sortingConfig.getCriteria().isEmpty()) {
            newRows = rows;
        } else {
            for(Record record : sortedRecords(sortingConfig)) {
                newRows.addAll(record.getRows());
            }
        }
        return new TestingGridState(columnModel, newRows, overlayModels);
    }
    
    private List<IndexedRow> sortedRows(SortingConfig sortingConfig) {
        if (sortingConfig.equals(SortingConfig.NO_SORTING)) {
            return indexedRows();
        }
        RowSorter rowSorter = new RowSorter(this, sortingConfig);
        List<IndexedRow> sortedIndexedRows = new ArrayList<>(indexedRows());
        Collections.sort(sortedIndexedRows, rowSorter);
        return sortedIndexedRows;
    }
    
    private List<Record> sortedRecords(SortingConfig sortingConfig) {
        if (sortingConfig.equals(SortingConfig.NO_SORTING)) {
            return records;
        }
        RecordSorter recordSorter = new RecordSorter(this, sortingConfig);
        List<Record> sortedRecords = new ArrayList<>(records);
        Collections.sort(sortedRecords, recordSorter);
        return sortedRecords;
    }

    @Override
    @JsonIgnore
    public DatamodelRunner getDatamodelRunner() {
        return new TestingDatamodelRunner();
    }

    @Override
    public GridState removeRows(RowFilter filter) {
        List<Row> newRows = indexedRows()
                .stream()
                .filter(ir -> !filter.filterRow(ir.getIndex(), ir.getRow()))
                .map(ir -> ir.getRow())
                .collect(Collectors.toList());
        return new TestingGridState(columnModel, newRows, overlayModels);
    }

    @Override
    public GridState removeRecords(RecordFilter filter) {
        List<Row> newRows = records
                .stream()
                .filter(r -> !filter.filterRecord(r))
                .flatMap(r -> r.getRows().stream())
                .collect(Collectors.toList());
        return new TestingGridState(columnModel, newRows, overlayModels);
    }

    @Override
    public <T extends Serializable> ChangeData<T> mapRows(RowFilter filter, RowChangeDataProducer<T> rowMapper) {
        // Check that the mapper is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        RowChangeDataProducer<T> deserializedMapper = TestingDatamodelRunner.serializeAndDeserialize(rowMapper);
        RowFilter deserializedFilter = TestingDatamodelRunner.serializeAndDeserialize(filter);
        
        Map<Long, T> changeData = new HashMap<>();
        Stream<IndexedRow> filteredRows = indexedRows().stream()
        .filter(ir -> deserializedFilter.filterRow(ir.getIndex(), ir.getRow()));
        if (deserializedMapper.getBatchSize() == 1) {
            filteredRows.forEach(ir -> changeData.put(ir.getIndex(), deserializedMapper.call(ir.getIndex(), ir.getRow())));
        } else {
            Iterator<List<IndexedRow>> batches = Iterators.partition(filteredRows.iterator(), deserializedMapper.getBatchSize());
            while (batches.hasNext()) {
                List<IndexedRow> batch = batches.next();
                List<T> results = deserializedMapper.call(batch);
                if (results.size() != batch.size()) {
                    throw new IllegalStateException(String.format("Change data producer returned %d results on a batch of %d rows", results.size(), batch.size()));
                }
                for(int i = 0; i != batch.size(); i++) {
                    changeData.put(batch.get(i).getIndex(), results.get(i));
                }
            }
        }
        return new TestingChangeData<T>(changeData);
    }
    
    @Override
    public <T extends Serializable> ChangeData<T> mapRecords(RecordFilter filter, RecordChangeDataProducer<T> recordMapper) {
        // Check that the mapper is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        RecordChangeDataProducer<T> deserializedMapper = TestingDatamodelRunner.serializeAndDeserialize(recordMapper);
        RecordFilter deserializedFilter = TestingDatamodelRunner.serializeAndDeserialize(filter);
        
        Map<Long, T> changeData = new HashMap<>();
        Stream<Record> filteredRecords = records.stream()
        .filter(ir -> deserializedFilter.filterRecord(ir));
        if (deserializedMapper.getBatchSize() == 1) {
            filteredRecords.forEach(record -> changeData.put(record.getStartRowId(), deserializedMapper.call(record)));
        } else {
            Iterator<List<Record>> batches = Iterators.partition(filteredRecords.iterator(), deserializedMapper.getBatchSize());
            while (batches.hasNext()) {
                List<Record> batch = batches.next();
                List<T> results = deserializedMapper.call(batch);
                if (results.size() != batch.size()) {
                    throw new IllegalStateException(String.format("Change data producer returned %d results on a batch of %d rows", results.size(), batch.size()));
                }
                for(int i = 0; i != batch.size(); i++) {
                    changeData.put(batch.get(i).getStartRowId(), results.get(i));
                }
            }
        }
        return new TestingChangeData<T>(changeData);
    }

    @Override
    public <T extends Serializable> GridState join(ChangeData<T> changeData, RowChangeDataJoiner<T> rowJoiner,
            ColumnModel newColumnModel) {
        // Check that the joiner is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        RowChangeDataJoiner<T> deserializedJoiner = TestingDatamodelRunner.serializeAndDeserialize(rowJoiner);
        
        List<Row> newRows = indexedRows()
                .stream()
                .map(ir -> deserializedJoiner.call(ir.getIndex(), ir.getRow(), changeData.get(ir.getIndex())))
                .collect(Collectors.toList());
        return new TestingGridState(newColumnModel, newRows, overlayModels);
    }

    @Override
    public <T extends Serializable> GridState join(ChangeData<T> changeData, RowChangeDataFlatJoiner<T> rowJoiner,
            ColumnModel newColumnModel) {
        // Check that the joiner is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        RowChangeDataFlatJoiner<T> deserializedJoiner = TestingDatamodelRunner.serializeAndDeserialize(rowJoiner);
        
        List<Row> newRows = indexedRows()
                .stream()
                .flatMap(ir -> deserializedJoiner.call(ir.getIndex(), ir.getRow(), changeData.get(ir.getIndex())).stream())
                .collect(Collectors.toList());
        return new TestingGridState(newColumnModel, newRows, overlayModels);
    }

    @Override
    public <T extends Serializable> GridState join(ChangeData<T> changeData, RecordChangeDataJoiner<T> recordJoiner,
            ColumnModel newColumnModel) {
        // Check that the joiner is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        RecordChangeDataJoiner<T> deserializedJoiner = TestingDatamodelRunner.serializeAndDeserialize(recordJoiner);
        
        List<Row> rows = records
                .stream()
                .flatMap(record -> deserializedJoiner.call(record, changeData.get(record.getStartRowId())).stream())
                .collect(Collectors.toList());
        return new TestingGridState(newColumnModel, rows, overlayModels);
    }

    @Override
    public GridState concatenate(GridState other) {
        ColumnModel merged = columnModel.merge(other.getColumnModel());
        
        List<Row> otherRows = other.collectRows().stream().map(r -> r.getRow()).collect(Collectors.toList());
        List<Row> newRows = new ArrayList<>(rows.size() + otherRows.size());
        newRows.addAll(rows);
        newRows.addAll(otherRows);
        
        Map<String, OverlayModel> newOverlayModels = new HashMap<>(other.getOverlayModels());
        newOverlayModels.putAll(overlayModels);
        return new TestingGridState(merged, newRows, newOverlayModels);
    }

}
