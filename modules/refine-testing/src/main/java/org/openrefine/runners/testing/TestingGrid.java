
package org.openrefine.runners.testing;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Iterators;
import org.testng.Assert;

import org.openrefine.browsing.facets.RecordAggregator;
import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.model.*;
import org.openrefine.model.Record;
import org.openrefine.model.changes.*;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.process.CompletedFuture;
import org.openrefine.process.FailingFuture;
import org.openrefine.process.ProgressReporter;
import org.openrefine.process.ProgressingFuture;
import org.openrefine.sorting.RecordSorter;
import org.openrefine.sorting.RowSorter;
import org.openrefine.sorting.SortingConfig;
import org.openrefine.util.CloseableIterator;
import org.openrefine.util.ParsingUtilities;

/**
 * A massively inefficient but very simple implementation of a Grid, for testing purposes.
 * 
 * @author Antonin Delpeuch
 *
 */
public class TestingGrid implements Grid {

    private ColumnModel columnModel;
    private Map<String, OverlayModel> overlayModels;
    private List<Row> rows;
    private List<IndexedRow> indexedRows;
    private List<Record> records;

    // the following is just to emulate the behaviour of a real implementation,
    // since in this testing one everything is always loaded in memory (cached).
    // This boolean indicates whether we pretend that the grid is cached or not.
    private boolean isCached = false;

    public TestingGrid(ColumnModel columnModel, List<Row> rows, Map<String, OverlayModel> overlayModels) {
        this(indexRows(rows), columnModel, overlayModels);
    }

    protected TestingGrid(List<IndexedRow> indexedRows, ColumnModel columnModel, Map<String, OverlayModel> overlayModels) {
        this.columnModel = columnModel;
        this.indexedRows = indexedRows;
        this.rows = indexedRows.stream().map(IndexedRow::getRow).collect(Collectors.toList());
        this.overlayModels = overlayModels;
        records = groupRowsIntoRecords(indexedRows, columnModel.getKeyColumnIndex());

        // Check that all rows have the same number of cells as the project has columns
        int expectedSize = columnModel.getColumns().size();
        if (!rows.stream().allMatch(r -> r.getCells().size() == expectedSize)) {
            throw new IllegalArgumentException("Rows are required to contain as many cells as there are columns");
        }
    }

    public static List<Record> groupRowsIntoRecords(List<IndexedRow> rows, int keyCellIndex) {
        return Record.groupIntoRecords(
                CloseableIterator.wrapping(rows.iterator()), keyCellIndex, false, Collections.emptyList())
                .toJavaList();
    }

    @Override
    public ColumnModel getColumnModel() {
        return columnModel;
    }

    @Override
    public Row getRow(long id) {
        return rows.get((int) id);
    }

    private static List<IndexedRow> indexRows(List<Row> rows) {
        return IntStream.range(0, rows.size()).mapToObj(i -> new IndexedRow((long) i, rows.get(i))).collect(Collectors.toList());
    }

    @Override
    public List<IndexedRow> getRowsAfter(long start, int limit) {
        return indexedRows.subList(
                Math.min((int) start, rows.size()),
                Math.min((int) start + limit, rows.size()));
    }

    @Override
    public List<IndexedRow> getRowsAfter(RowFilter filter, long start, int limit) {
        // Check that the filter is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        RowFilter deserializedFilter = TestingRunner.serializeAndDeserialize(filter);

        return indexedRows.stream()
                .filter(tuple -> deserializedFilter.filterRow(tuple.getLogicalIndex(), tuple.getRow()) && tuple.getIndex() >= start)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<IndexedRow> getRowsBefore(long end, int limit) {
        int actualEnd = Math.min((int) end, rows.size());
        return indexedRows.subList(
                Math.max(actualEnd - limit, 0),
                actualEnd);
    }

    @Override
    public List<IndexedRow> getRowsBefore(RowFilter filter, long end, int limit) {
        // Check that the filter is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        RowFilter deserializedFilter = TestingRunner.serializeAndDeserialize(filter);

        // this is really not efficient but that is not the point of this implementation: it should just be correct
        List<IndexedRow> matchingRows = indexedRows.stream()
                .filter(tuple -> deserializedFilter.filterRow(tuple.getLogicalIndex(), tuple.getRow()) && tuple.getIndex() < end)
                .collect(Collectors.toList());
        return matchingRows.subList(Math.max(0, matchingRows.size() - limit), matchingRows.size());
    }

    @Override
    public List<IndexedRow> collectRows() {
        return indexedRows;
    }

    @Override
    public Record getRecord(long id) {
        List<Record> matching = getRecordsAfter(id, 1);
        if (matching.isEmpty() || matching.get(0).getStartRowId() != id) {
            throw new IllegalArgumentException(String.format("No record with id %d", id));
        }
        return matching.get(0);
    }

    @Override
    public List<Record> getRecordsAfter(long start, int limit) {
        return records
                .stream()
                .filter(record -> record.getStartRowId() >= start)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Record> getRecordsAfter(RecordFilter filter, long start, int limit) {
        // Check that the filter is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        RecordFilter deserializedFilter = TestingRunner.serializeAndDeserialize(filter);
        return records
                .stream()
                .filter(record -> record.getStartRowId() >= start && deserializedFilter.filterRecord(record))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Record> getRecordsBefore(long end, int limit) {
        return getRecordsBefore(RecordFilter.ANY_RECORD, end, limit);
    }

    @Override
    public List<Record> getRecordsBefore(RecordFilter filter, long end, int limit) {
        // Check that the filter is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        RecordFilter deserializedFilter = TestingRunner.serializeAndDeserialize(filter);
        List<Record> matching = records
                .stream()
                .filter(record -> record.getStartRowId() < end && deserializedFilter.filterRecord(record))
                .collect(Collectors.toList());
        return matching.subList(Math.max(0, matching.size() - limit), matching.size());
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
        return indexedRows
                .stream()
                .filter(tuple -> filter.filterRow(tuple.getIndex(), tuple.getRow()))
                .count();
    }

    @Override
    public ApproxCount countMatchingRowsApprox(RowFilter filter, long limit) {
        long matching = indexedRows
                .stream()
                .limit(limit)
                .filter(tuple -> filter.filterRow(tuple.getIndex(), tuple.getRow()))
                .count();
        return new ApproxCount(Math.min(limit, rows.size()), matching, limit <= rows.size());
    }

    @Override
    public long countMatchingRecords(RecordFilter filter) {
        return records
                .stream()
                .filter(record -> filter.filterRecord(record))
                .count();
    }

    @Override
    public ApproxCount countMatchingRecordsApprox(RecordFilter filter, long limit) {
        long matching = records
                .stream()
                .limit(limit)
                .filter(record -> filter.filterRecord(record))
                .count();
        return new ApproxCount(Math.min(limit, records.size()), matching, limit <= records.size());
    }

    @Override
    public Map<String, OverlayModel> getOverlayModels() {
        return overlayModels;
    }

    @Override
    public void saveToFile(File file) throws IOException {
        File gridPath = new File(file, Grid.GRID_PATH);
        File metadataPath = new File(file, Grid.METADATA_PATH);

        gridPath.mkdirs();
        File partFile = new File(gridPath, "part-00000.gz");
        try (FileOutputStream fos = new FileOutputStream(partFile);
                GZIPOutputStream gos = new GZIPOutputStream(fos);) {
            for (IndexedRow row : indexedRows) {
                ParsingUtilities.saveWriter.writeValue(gos, row);
                gos.write('\n');
            }
        }

        ParsingUtilities.saveWriter.writeValue(metadataPath, this);

        File completionMarker = new File(gridPath, Runner.COMPLETION_MARKER_FILE_NAME);
        try (FileOutputStream fosCompletion = new FileOutputStream(completionMarker)) {
            Writer writer = new OutputStreamWriter(fosCompletion);
            writer.close();
        }
    }

    @Override
    public ProgressingFuture<Void> saveToFileAsync(File file) {
        try {
            saveToFile(file);
        } catch (IOException e) {
            return new FailingFuture<>(e);
        }
        return new CompletedFuture<>(null);
    }

    protected void saveToFile(File file, Optional<ProgressReporter> progressReporter) throws IOException {

    }

    @Override
    public <T extends Serializable> T aggregateRows(RowAggregator<T> aggregator, T initialState) {
        return aggregateRowsApprox(aggregator, initialState, rows.size()).getState();
    }

    @Override
    public <T extends Serializable> T aggregateRecords(RecordAggregator<T> aggregator, T initialState) {
        return aggregateRecordsApprox(aggregator, initialState, records.size()).getState();
    }

    @Override
    public <T extends Serializable> PartialAggregation<T> aggregateRowsApprox(RowAggregator<T> aggregator, T initialState, long maxRows) {
        initialState = TestingRunner.serializeAndDeserialize(initialState);
        aggregator = TestingRunner.serializeAndDeserialize(aggregator);
        // Artificially split the grid in two, in order to use the `sum` method
        // of RowAggregator.
        long split = rowCount() / 2;
        T statesA = initialState;
        T statesB = initialState;
        long count = 0;
        for (IndexedRow row : indexedRows) {
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

        return new PartialAggregation<T>(aggregator.sum(statesA, statesB), count, count == maxRows);
    }

    @Override
    public <T extends Serializable> PartialAggregation<T> aggregateRecordsApprox(RecordAggregator<T> aggregator, T initialState,
            long maxRecords) {
        initialState = TestingRunner.serializeAndDeserialize(initialState);
        aggregator = TestingRunner.serializeAndDeserialize(aggregator);
        // Artificially split the grid in two, in order to use the `sum` method
        // of FacetAggregator.
        long split = rowCount() / 2;
        T statesA = initialState;
        T statesB = initialState;
        long count = 0;
        for (Record record : records) {
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

        return new PartialAggregation<T>(aggregator.sum(statesA, statesB), count, count == maxRecords);
    }

    @Override
    public Grid mapRows(RowMapper mapper, ColumnModel newColumnModel) {
        // Check that the mapper is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        mapper = TestingRunner.serializeAndDeserialize(mapper);

        List<Row> rows = new ArrayList<>(this.rows.size());
        for (IndexedRow indexedRow : indexedRows) {
            Row row = mapper.call(indexedRow.getIndex(), indexedRow.getRow());
            if (row.getCells().size() != newColumnModel.getColumns().size()) {
                Assert.fail(String.format("Row size (%d) inconsistent with supplied column model (%s)",
                        row.getCells().size(), newColumnModel.getColumns()));
            }
            rows.add(row);
        }
        return new TestingGrid(newColumnModel, rows, overlayModels);
    }

    @Override
    public Grid flatMapRows(RowFlatMapper mapper, ColumnModel newColumnModel) {
        // Check that the mapper is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        mapper = TestingRunner.serializeAndDeserialize(mapper);

        List<Row> rows = new ArrayList<>(this.rows.size());
        for (IndexedRow indexedRow : indexedRows) {
            List<Row> rowBatch = mapper.call(indexedRow.getIndex(), indexedRow.getRow());
            for (Row row : rowBatch) {
                if (row.getCells().size() != newColumnModel.getColumns().size()) {
                    Assert.fail(String.format("Row size (%d) inconsistent with supplied column model (%s)",
                            row.getCells().size(), newColumnModel.getColumns()));
                }
            }
            rows.addAll(rowBatch);
        }
        return new TestingGrid(newColumnModel, rows, overlayModels);
    }

    @Override
    public <S extends Serializable> Grid mapRows(RowScanMapper<S> mapper, ColumnModel newColumnModel) {
        // Check that the mapper is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        mapper = TestingRunner.serializeAndDeserialize(mapper);

        S currentState = mapper.unit();
        List<Row> rows = new ArrayList<>(this.rows.size());
        for (IndexedRow indexedRow : indexedRows) {
            Row row = mapper.map(currentState, indexedRow.getIndex(), indexedRow.getRow());
            currentState = mapper.combine(currentState, mapper.feed(indexedRow.getIndex(), indexedRow.getRow()));
            if (row.getCells().size() != newColumnModel.getColumns().size()) {
                Assert.fail(String.format("Row size (%d) inconsistent with supplied column model (%s)",
                        row.getCells().size(), newColumnModel.getColumns()));
            }
            rows.add(row);
        }
        return new TestingGrid(newColumnModel, rows, overlayModels);
    }

    @Override
    public Grid mapRecords(RecordMapper mapper, ColumnModel newColumnModel) {
        // Check that the mapper is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        mapper = TestingRunner.serializeAndDeserialize(mapper);

        List<Row> rows = new ArrayList<>(this.rows.size());
        for (Record record : records) {
            List<Row> addedRows = mapper.call(record);
            for (Row row : addedRows) {
                if (row.getCells().size() != newColumnModel.getColumns().size()) {
                    Assert.fail("Row size inconsistent with supplied column model");
                }
            }
            rows.addAll(addedRows);
        }
        return new TestingGrid(newColumnModel, rows, overlayModels);
    }

    @Override
    public CloseableIterator<IndexedRow> iterateRows(RowFilter filter) {
        return CloseableIterator.wrapping(indexedRows
                .stream()
                .filter(ir -> filter.filterRow(ir.getLogicalIndex(), ir.getRow()))
                .iterator());
    }

    @Override
    public CloseableIterator<Record> iterateRecords(RecordFilter filter) {
        return CloseableIterator.wrapping(records
                .stream()
                .filter(filter::filterRecord)
                .iterator());
    }

    @Override
    public Grid withOverlayModels(Map<String, OverlayModel> overlayModel) {
        return new TestingGrid(columnModel, rows, overlayModel);
    }

    @Override
    public Grid withColumnModel(ColumnModel newColumnModel) {
        return new TestingGrid(newColumnModel, rows, overlayModels);
    }

    @Override
    public Grid reorderRows(SortingConfig sortingConfig, boolean permanent) {
        List<IndexedRow> newRows = sortedRows(sortingConfig);
        if (permanent) {
            return new TestingGrid(columnModel, newRows.stream().map(IndexedRow::getRow).collect(Collectors.toList()), overlayModels);
        } else {
            List<IndexedRow> indexed = IntStream.range(0, newRows.size())
                    .mapToObj(i -> new IndexedRow((long) i, newRows.get(i).getLogicalIndex(), newRows.get(i).getRow()))
                    .collect(Collectors.toList());
            return new TestingGrid(indexed, columnModel, overlayModels);
        }
    }

    @Override
    public Grid reorderRecords(SortingConfig sortingConfig, boolean permanent) {
        List<IndexedRow> newRows = new ArrayList<>(rows.size());
        if (sortingConfig.getCriteria().isEmpty()) {
            newRows = indexedRows;
        } else {
            for (Record record : sortedRecords(sortingConfig)) {
                for (IndexedRow row : record.getIndexedRows()) {
                    newRows.add(new IndexedRow(newRows.size(), permanent ? null : row.getLogicalIndex(), row.getRow()));
                }
            }
        }
        return new TestingGrid(newRows, columnModel, overlayModels);
    }

    private List<IndexedRow> sortedRows(SortingConfig sortingConfig) {
        if (sortingConfig.equals(SortingConfig.NO_SORTING)) {
            return indexedRows;
        }
        RowSorter rowSorter = new RowSorter(this, sortingConfig);
        List<IndexedRow> sortedIndexedRows = new ArrayList<>(indexedRows);
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
    public Runner getRunner() {
        return new TestingRunner();
    }

    @Override
    public Grid removeRows(RowFilter filter) {
        List<Row> newRows = indexedRows
                .stream()
                .filter(ir -> !filter.filterRow(ir.getLogicalIndex(), ir.getRow()))
                .map(ir -> ir.getRow())
                .collect(Collectors.toList());
        return new TestingGrid(columnModel, newRows, overlayModels);
    }

    @Override
    public Grid removeRecords(RecordFilter filter) {
        List<Row> newRows = records
                .stream()
                .filter(r -> !filter.filterRecord(r))
                .flatMap(r -> r.getRows().stream())
                .collect(Collectors.toList());
        return new TestingGrid(columnModel, newRows, overlayModels);
    }

    @Override
    public <T> ChangeData<T> mapRows(RowFilter filter, RowChangeDataProducer<T> rowMapper, Optional<ChangeData<T>> incompleteChangeData) {
        // Check that the mapper is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        RowChangeDataProducer<T> deserializedMapper = TestingRunner.serializeAndDeserialize(rowMapper);
        RowFilter deserializedFilter = TestingRunner.serializeAndDeserialize(filter);

        Map<Long, IndexedData<T>> changeData = new HashMap<>();
        Stream<IndexedRow> filteredRows = indexedRows.stream()
                .filter(ir -> deserializedFilter.filterRow(ir.getIndex(), ir.getRow()));

        // add data from any previously computed change data
        if (incompleteChangeData.isPresent()) {
            filteredRows.forEach(ir -> {
                IndexedData<T> precomputed = incompleteChangeData.get().get(ir.getIndex());
                if (precomputed != null && !precomputed.isPending()) {
                    changeData.put(ir.getIndex(), precomputed);
                }
            });
            filteredRows = indexedRows.stream()
                    .filter(ir -> deserializedFilter.filterRow(ir.getIndex(), ir.getRow())
                            && incompleteChangeData.get().get(ir.getIndex()).isPending());
        }

        // compute missing change data items
        if (deserializedMapper.getBatchSize() == 1) {
            filteredRows.forEach(ir -> {
                changeData.put(ir.getIndex(), new IndexedData<>(ir.getIndex(), deserializedMapper.call(ir.getIndex(), ir.getRow())));
            });
        } else {
            Iterator<List<IndexedRow>> batches = Iterators.partition(filteredRows.iterator(), deserializedMapper.getBatchSize());
            while (batches.hasNext()) {
                List<IndexedRow> batch = batches.next();
                List<T> results = deserializedMapper.callRowBatch(batch);
                if (results.size() != batch.size()) {
                    throw new IllegalStateException(
                            String.format("Change data producer returned %d results on a batch of %d rows", results.size(), batch.size()));
                }
                for (int i = 0; i != batch.size(); i++) {
                    long index = batch.get(i).getIndex();
                    changeData.put(index, new IndexedData<>(index, results.get(i)));
                }
            }
        }
        return new TestingChangeData<T>(changeData, true);
    }

    @Override
    public <T> ChangeData<T> mapRecords(RecordFilter filter, RecordChangeDataProducer<T> recordMapper,
            Optional<ChangeData<T>> incompleteChangeData) {
        // Check that the mapper is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        RecordChangeDataProducer<T> deserializedMapper = TestingRunner.serializeAndDeserialize(recordMapper);
        RecordFilter deserializedFilter = TestingRunner.serializeAndDeserialize(filter);

        Map<Long, IndexedData<T>> changeData = new HashMap<>();
        Stream<Record> filteredRecords = records.stream()
                .filter(ir -> deserializedFilter.filterRecord(ir));

        // add data from any previously computed change data
        if (incompleteChangeData.isPresent()) {
            filteredRecords.forEach(ir -> {
                IndexedData<T> precomputed = incompleteChangeData.get().get(ir.getStartRowId());
                if (precomputed != null && !precomputed.isPending()) {
                    changeData.put(ir.getStartRowId(), precomputed);
                }
            });
            filteredRecords = records.stream()
                    .filter(ir -> deserializedFilter.filterRecord(ir) && incompleteChangeData.get().get(ir.getStartRowId()).isPending());
        }

        if (deserializedMapper.getBatchSize() == 1) {
            filteredRecords.forEach(record -> changeData.put(record.getStartRowId(),
                    new IndexedData<>(record.getStartRowId(), deserializedMapper.call(record))));
        } else {
            Iterator<List<Record>> batches = Iterators.partition(filteredRecords.iterator(), deserializedMapper.getBatchSize());
            while (batches.hasNext()) {
                List<Record> batch = batches.next();
                List<T> results = deserializedMapper.callRecordBatch(batch);
                if (results.size() != batch.size()) {
                    throw new IllegalStateException(
                            String.format("Change data producer returned %d results on a batch of %d rows", results.size(), batch.size()));
                }
                for (int i = 0; i != batch.size(); i++) {
                    changeData.put(batch.get(i).getStartRowId(), new IndexedData<>(batch.get(i).getStartRowId(), results.get(i)));
                }
            }
        }
        return new TestingChangeData<>(changeData, true);
    }

    @Override
    public <T> Grid join(ChangeData<T> changeData, RowChangeDataJoiner<T> rowJoiner,
            ColumnModel newColumnModel) {
        // Check that the joiner is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        RowChangeDataJoiner<T> deserializedJoiner = TestingRunner.serializeAndDeserialize(rowJoiner);

        List<Row> newRows = indexedRows
                .stream()
                .map(ir -> deserializedJoiner.call(ir.getRow(), changeData.get(ir.getIndex())))
                .collect(Collectors.toList());
        return new TestingGrid(newColumnModel, newRows, overlayModels);
    }

    @Override
    public <T> Grid join(ChangeData<T> changeData, RowChangeDataFlatJoiner<T> rowJoiner,
            ColumnModel newColumnModel) {
        // Check that the joiner is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        RowChangeDataFlatJoiner<T> deserializedJoiner = TestingRunner.serializeAndDeserialize(rowJoiner);

        List<Row> newRows = indexedRows
                .stream()
                .flatMap(ir -> {
                    IndexedData<T> indexedData = changeData.get(ir.getIndex());
                    return deserializedJoiner.call(
                            ir.getRow(),
                            indexedData != null ? indexedData : new IndexedData<>(ir.getIndex()))
                            .stream();
                })
                .collect(Collectors.toList());
        return new TestingGrid(newColumnModel, newRows, overlayModels);
    }

    @Override
    public <T> Grid join(ChangeData<T> changeData, RecordChangeDataJoiner<T> recordJoiner,
            ColumnModel newColumnModel) {
        // Check that the joiner is serializable as it is required by the interface,
        // even if this implementation does not rely on it.
        RecordChangeDataJoiner<T> deserializedJoiner = TestingRunner.serializeAndDeserialize(recordJoiner);

        List<Row> rows = records
                .stream()
                .flatMap(record -> deserializedJoiner.call(record, changeData.get(record.getStartRowId())).stream())
                .collect(Collectors.toList());
        return new TestingGrid(newColumnModel, rows, overlayModels);
    }

    @Override
    public Grid concatenate(Grid other) {
        ColumnModel merged = columnModel.merge(other.getColumnModel());

        List<Row> otherRows = other.collectRows().stream().map(r -> r.getRow()).collect(Collectors.toList());
        List<Row> newRows = new ArrayList<>(rows.size() + otherRows.size());
        newRows.addAll(rows);
        newRows.addAll(otherRows);

        Map<String, OverlayModel> newOverlayModels = new HashMap<>(other.getOverlayModels());
        newOverlayModels.putAll(overlayModels);
        return new TestingGrid(merged, newRows, newOverlayModels);
    }

    @Override
    public boolean isCached() {
        return isCached;
    }

    @Override
    public void uncache() {
        isCached = false;
    }

    @Override
    public ProgressingFuture<Boolean> cacheAsync() {
        isCached = true;
        return new CompletedFuture<Boolean>(true);
    }

    @Override
    public boolean cache() {
        isCached = true;
        return true;
    }

}
