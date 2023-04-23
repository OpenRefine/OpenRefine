
package org.openrefine.runners.local;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.openrefine.browsing.facets.RecordAggregator;
import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.model.*;
import org.openrefine.model.Record;
import org.openrefine.model.changes.*;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.process.ProgressingFuture;
import org.openrefine.process.ProgressingFutures;
import org.openrefine.runners.local.pll.PLL;
import org.openrefine.runners.local.pll.PairPLL;
import org.openrefine.runners.local.pll.RecordPLL;
import org.openrefine.runners.local.pll.Tuple2;
import org.openrefine.runners.local.pll.partitioning.LongRangePartitioner;
import org.openrefine.runners.local.pll.partitioning.Partitioner;
import org.openrefine.runners.local.pll.util.QueryTree;
import org.openrefine.sorting.RecordSorter;
import org.openrefine.sorting.RowSorter;
import org.openrefine.sorting.SortingConfig;
import org.openrefine.util.CloseableIterator;
import org.openrefine.util.ParsingUtilities;

/**
 * A PLL-based implementation of a Grid.
 * 
 * @author Antonin Delpeuch
 *
 */
@JsonIgnoreType
public class LocalGrid implements Grid {

    protected final LocalRunner runner;
    protected final PairPLL<Long, IndexedRow> grid;
    protected final ColumnModel columnModel;
    protected final Map<String, OverlayModel> overlayModels;

    // flag to indicate whether the grid was constructed from a PLL of rows or records.
    // This helps determine which one should be cached, to make sure both rows and record
    // access is fast while only caching a single PLL.
    protected final boolean constructedFromRows;

    // initialized lazily
    protected PairPLL<Long, Record> records;
    // also initialized lazily: can be used to cache the number of records if it is known already
    // (for instance if this grid was obtained from another one by applying an operation which preserves records)
    protected long cachedRecordCount = -1;

    /**
     * Constructs a grid, supplying all required fields.
     * 
     * @param runner
     * @param columnModel
     * @param grid
     * @param overlayModels
     */
    public LocalGrid(
            LocalRunner runner,
            ColumnModel columnModel,
            PairPLL<Long, IndexedRow> grid,
            Map<String, OverlayModel> overlayModels,
            long cachedRecordCount) {
        this.runner = runner;
        this.grid = grid;
        this.columnModel = columnModel;
        this.overlayModels = overlayModels;
        this.records = null;
        this.constructedFromRows = true;
        this.cachedRecordCount = cachedRecordCount;
        if (grid.getPartitioner().isEmpty()) {
            throw new IllegalArgumentException("No partitioner supplied for the rows PLL");
        }
    }

    /**
     * Convenience constructor to construct a grid from a PLL of a slightly different type.
     */
    public LocalGrid(
            LocalRunner runner,
            PairPLL<Long, Row> grid,
            ColumnModel columnModel,
            Map<String, OverlayModel> overlayModels,
            long cachedRecordCount) {
        this.runner = runner;
        if (grid.getPartitioner().isEmpty()) {
            throw new IllegalArgumentException("No partitioner supplied for the rows PLL");
        }
        this.grid = grid.mapToPair(tuple -> new Tuple2<>(tuple.getKey(), new IndexedRow(tuple.getKey(), tuple.getValue())),
                "IndexedRow to pair")
                .withPartitioner(grid.getPartitioner());
        this.columnModel = columnModel;
        this.overlayModels = overlayModels;
        this.records = null;
        this.constructedFromRows = true;
        this.cachedRecordCount = cachedRecordCount;
    }

    /**
     * Constructs a grid from a grid of records.
     *
     * @param records
     *            the PLL of records for the current grid, which is assumed to have consistent indexing
     * @param runner
     * @param columnModel
     * @param overlayModels
     */
    public LocalGrid(
            PairPLL<Long, Record> records,
            LocalRunner runner,
            ColumnModel columnModel,
            Map<String, OverlayModel> overlayModels,
            long rowCount) {
        this.runner = runner;
        this.columnModel = columnModel;
        this.overlayModels = overlayModels;
        this.records = records;
        if (records.getPartitioner().isEmpty()) {
            throw new IllegalArgumentException("Missing partitioner for records PLL");
        }
        this.grid = PairPLL.assumeIndexed(records.values()
                .flatMap(record -> CloseableIterator.wrapping(record.getIndexedRows().iterator()), "flatten records to rows")
                .mapToPair(indexedRow -> Tuple2.of(indexedRow.getIndex(), indexedRow), "to pair"), rowCount);
        this.constructedFromRows = false;
    }

    protected PairPLL<Long, Record> records() {
        if (records == null) {
            records = RecordPLL.groupIntoRecords(grid, columnModel.getKeyColumnIndex());
        }
        return records;
    }

    protected PLL<IndexedRow> indexedRows() {
        return grid.values();
    }

    @Override
    public LocalRunner getRunner() {
        return runner;
    }

    @Override
    public ColumnModel getColumnModel() {
        return columnModel;
    }

    @Override
    public Grid withColumnModel(ColumnModel newColumnModel) {
        if (constructedFromRows) {
            return new LocalGrid(runner, newColumnModel, grid, overlayModels, cachedRecordCount);
        } else {
            return new LocalGrid(records, runner, newColumnModel, overlayModels, grid.count());
        }
    }

    @Override
    public Row getRow(long id) {
        // because `get` does not assume that the PLL is sorted,
        // it must actually scan at least one entire partition,
        // which is unnecessary since we know it is sorted and there
        // is at most one result. So we use `getRange` instead.
        List<Tuple2<Long, IndexedRow>> rows = grid.getRangeAfter(id, 1, Comparator.naturalOrder());
        if (rows.size() == 0) {
            throw new IndexOutOfBoundsException(String.format("Row id %d not found", id));
        } else if (rows.size() > 1) {
            throw new IllegalStateException(String.format("Found %d rows at index %d", rows.size(), id));
        } else {
            return rows.get(0).getValue().getRow();
        }
    }

    @Override
    public List<IndexedRow> getRowsAfter(long start, int limit) {
        return grid.getRangeAfter(start, limit, Comparator.naturalOrder())
                .stream()
                .map(Tuple2::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public List<IndexedRow> getRowsAfter(RowFilter filter, long start, int limit) {
        PairPLL<Long, IndexedRow> filteredRows = grid
                .filter(tuple -> filter.filterRow(tuple.getValue().getLogicalIndex(), tuple.getValue().getRow()));
        return filteredRows
                .getRangeAfter(start, limit, Comparator.naturalOrder())
                .stream()
                .map(Tuple2::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public List<IndexedRow> getRowsBefore(long end, int limit) {
        return grid.getRangeBefore(end, limit, Comparator.naturalOrder())
                .stream()
                .map(Tuple2::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public List<IndexedRow> getRowsBefore(RowFilter filter, long end, int limit) {
        return grid
                .filter(tuple -> filter.filterRow(tuple.getValue().getLogicalIndex(), tuple.getValue().getRow()))
                .getRangeBefore(end, limit, Comparator.naturalOrder())
                .stream()
                .map(Tuple2::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public List<IndexedRow> getRows(List<Long> rowIndices) {
        io.vavr.collection.Map<Long, IndexedRow> results = grid.getByKeys(rowIndices.stream().collect(Collectors.toSet()))
                .toMap(t -> new io.vavr.Tuple2(t.getKey(), t.getValue()));

        return rowIndices.stream()
                .map(i -> results.get(i).getOrNull())
                .collect(Collectors.toList());
    }

    @Override
    public CloseableIterator<IndexedRow> iterateRows(RowFilter filter) {
        return grid
                .filter(tuple -> filter.filterRow(tuple.getValue().getLogicalIndex(), tuple.getValue().getRow()))
                .values()
                .iterator();
    }

    @Override
    public long countMatchingRows(RowFilter filter) {
        return grid.filter(tuple -> filter.filterRow(tuple.getValue().getLogicalIndex(), tuple.getValue().getRow())).count();
    }

    @Override
    public ApproxCount countMatchingRowsApprox(RowFilter filter, long limit) {
        PartialAggregation<Long> partialAggregation = sample(indexedRows(), limit, 0L,
                (matching, indexedRow) -> matching + (filter.filterRow(indexedRow.getLogicalIndex(), indexedRow.getRow()) ? 1 : 0),
                (a, b) -> a + b);
        return new ApproxCount(partialAggregation.getProcessed(), partialAggregation.getState(), partialAggregation.limitReached());
    }

    @Override
    public List<IndexedRow> collectRows() {
        return grid.values().collect().toJavaList();
    }

    @Override
    public Record getRecord(long id) {
        List<Record> rows = records().get(id).toJavaList();
        if (rows.size() == 0) {
            throw new IllegalArgumentException(String.format("Record id %d not found", id));
        } else if (rows.size() > 1) {
            throw new IllegalStateException(String.format("Found %d records at index %d", rows.size(), id));
        } else {
            return rows.get(0);
        }
    }

    @Override
    public List<Record> getRecordsAfter(long start, int limit) {
        return records()
                .getRangeAfter(start, limit, Comparator.naturalOrder())
                .stream()
                .map(tuple -> tuple.getValue())
                .collect(Collectors.toList());
    }

    @Override
    public List<Record> getRecordsAfter(RecordFilter filter, long start, int limit) {
        PairPLL<Long, Record> records = records();
        return records
                .filter(tuple -> filter.filterRecord(tuple.getValue()))
                .getRangeAfter(start, limit, Comparator.naturalOrder())
                .stream()
                .map(tuple -> tuple.getValue())
                .collect(Collectors.toList());
    }

    @Override
    public List<Record> getRecordsBefore(long end, int limit) {
        return records()
                .getRangeBefore(end, limit, Comparator.naturalOrder())
                .stream()
                .map(Tuple2::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public List<Record> getRecordsBefore(RecordFilter filter, long end, int limit) {
        return records()
                .filter(tuple -> filter.filterRecord(tuple.getValue()))
                .getRangeBefore(end, limit, Comparator.naturalOrder())
                .stream()
                .map(Tuple2::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public CloseableIterator<Record> iterateRecords(RecordFilter filter) {
        PLL<Record> records = records().values();
        return records
                .filter(tuple -> filter.filterRecord(tuple))
                .iterator();
    }

    @Override
    public long countMatchingRecords(RecordFilter filter) {
        return records().filter(tuple -> filter.filterRecord(tuple.getValue())).count();
    }

    @Override
    public ApproxCount countMatchingRecordsApprox(RecordFilter filter, long limit) {
        PLL<Record> records = records().values();
        PartialAggregation<Long> partialAggregation = sample(records, limit, 0L,
                (matching, record) -> matching + (filter.filterRecord(record) ? 1 : 0),
                (a, b) -> a + b);
        return new ApproxCount(partialAggregation.getProcessed(), partialAggregation.getState(), partialAggregation.limitReached());
    }

    @Override
    public List<Record> collectRecords() {
        return records().values().collect().toJavaList();
    }

    @Override
    public long rowCount() {
        return grid.count();
    }

    @Override
    public long recordCount() {
        if (cachedRecordCount >= 0) {
            return cachedRecordCount;
        } else {
            return cachedRecordCount = records().count();
        }
    }

    @Override
    public Map<String, OverlayModel> getOverlayModels() {
        return overlayModels;
    }

    @Override
    public ProgressingFuture<Void> saveToFileAsync(File file) {
        File metadataFile = new File(file, METADATA_PATH);
        File gridFile = new File(file, GRID_PATH);

        return ProgressingFutures.transform(grid
                .values()
                .map(LocalGrid::serializeIndexedRow, "serialize indexed row")
                .saveAsTextFileAsync(gridFile.getAbsolutePath(), runner.defaultParallelism, true, false),
                v -> {
                    try {
                        ParsingUtilities.saveWriter.writeValue(metadataFile, getMetadata());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    return null;
                }, runner.getPLLContext().getExecutorService());
    }

    @Override
    public void saveToFile(File file) throws IOException {
        ProgressingFuture<Void> future = saveToFileAsync(file);
        try {
            future.get();
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new IOException("The operation was interrupted", e);
        } catch (ExecutionException e) {
            throw new IOException(e.getCause());
        }
    }

    protected Metadata getMetadata() {
        Metadata metadata = new Metadata();
        metadata.columnModel = columnModel;
        metadata.overlayModels = overlayModels;
        metadata.rowCount = grid.hasCachedPartitionSizes() ? grid.count() : -1;
        metadata.recordCount = records != null && records.hasCachedPartitionSizes() ? records.count() : cachedRecordCount;
        return metadata;
    }

    protected static String serializeIndexedRow(IndexedRow indexedRow) {
        try {
            return ParsingUtilities.saveWriter.writeValueAsString(indexedRow);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <T extends Serializable> T aggregateRows(RowAggregator<T> aggregator, T initialState) {
        return grid
                .values()
                .aggregate(initialState,
                        (s, t) -> aggregator.withRow(s, t.getLogicalIndex(), t.getRow()),
                        aggregator::sum);
    }

    @Override
    public <T extends Serializable> T aggregateRecords(RecordAggregator<T> aggregator, T initialState) {
        return records().aggregate(initialState,
                (s, t) -> aggregator.withRecord(s, t.getValue()),
                aggregator::sum);
    }

    @Override
    public <T extends Serializable> PartialAggregation<T> aggregateRowsApprox(RowAggregator<T> aggregator, T initialState, long maxRows) {
        return sample(indexedRows(), maxRows, initialState,
                (state, indexedRow) -> aggregator.withRow(state, indexedRow.getLogicalIndex(), indexedRow.getRow()),
                aggregator::sum);
    }

    @Override
    public <T extends Serializable> PartialAggregation<T> aggregateRecordsApprox(RecordAggregator<T> aggregator, T initialState,
            long maxRecords) {
        PLL<Record> records = records().values();
        return sample(records, maxRecords, initialState,
                aggregator::withRecord,
                aggregator::sum);
    }

    @Override
    public Grid withOverlayModels(Map<String, OverlayModel> newOverlayModels) {
        if (constructedFromRows) {
            return new LocalGrid(runner, columnModel, grid, newOverlayModels, cachedRecordCount);
        } else {
            return new LocalGrid(records, runner, columnModel, newOverlayModels, grid.count());
        }
    }

    @Override
    public Grid mapRows(RowMapper mapper, ColumnModel newColumnModel) {
        PairPLL<Long, Row> newGrid = grid.mapValues((i, r) -> mapper.call(r.getLogicalIndex(), r.getRow()), "apply RowMapper");
        return new LocalGrid(runner, newGrid, newColumnModel, overlayModels, mapper.preservesRecordStructure() ? cachedRecordCount : -1);
    }

    @Override
    public Grid flatMapRows(RowFlatMapper mapper, ColumnModel newColumnModel) {
        PairPLL<Long, Row> newGrid = grid.values()
                .flatMap(tuple -> CloseableIterator.wrapping(
                        mapper.call(tuple.getLogicalIndex(), tuple.getRow()).iterator()), "apply FlatRowMapper")
                .zipWithIndex();
        return new LocalGrid(runner, newGrid, newColumnModel, overlayModels, -1);
    }

    @Override
    public <S extends Serializable> Grid mapRows(RowScanMapper<S> mapper, ColumnModel newColumnModel) {
        PLL<Tuple2<Long, Row>> newGrid = grid.scanMap(
                mapper.unit(),
                tuple -> mapper.feed(tuple.getValue().getLogicalIndex(), tuple.getValue().getRow()),
                (s1, s2) -> mapper.combine(s1, s2),
                (s, tuple) -> Tuple2.of(tuple.getKey(), mapper.map(s, tuple.getValue().getLogicalIndex(), tuple.getValue().getRow())));
        PairPLL<Long, Row> paired = new PairPLL<>(newGrid, grid.getPartitioner());

        return new LocalGrid(runner, paired, newColumnModel, overlayModels, mapper.preservesRecordStructure() ? cachedRecordCount : -1);
    }

    @Override
    public Grid mapRecords(RecordMapper mapper, ColumnModel newColumnModel) {
        if (mapper.preservesRecordStructure()) {
            PairPLL<Long, Record> newRecords = records()
                    .mapValues(
                            (index, record) -> new Record(record.getStartRowId(), record.getOriginalStartRowId(), mapper.call(record)),
                            "apply RecordMapper, preserving records");
            return new LocalGrid(newRecords, runner, newColumnModel, overlayModels, grid.count());
        } else {
            PairPLL<Long, Row> grid = records()
                    .values()
                    .flatMap(record -> CloseableIterator.wrapping(mapper.call(record).iterator()),
                            "apply RecordMapper, not preserving records")
                    .zipWithIndex();
            return new LocalGrid(runner, grid, newColumnModel, overlayModels, -1);
        }
    }

    @Override
    public Grid reorderRows(SortingConfig sortingConfig, boolean permanent) {
        RowSorter rowSorter = new RowSorter(this, sortingConfig);
        if (permanent) {
            PairPLL<Long, Row> newRows = indexedRows()
                    .sort(rowSorter)
                    .map(IndexedRow::getRow, "apply IndexRow::getRow")
                    .zipWithIndex();
            return new LocalGrid(runner, newRows, columnModel, overlayModels, -1);
        } else {
            PairPLL<Long, IndexedRow> newRows = indexedRows()
                    .sort(rowSorter)
                    .zipWithIndex()
                    .mapValues((newIndex, indexedRow) -> new IndexedRow(newIndex, indexedRow.getLogicalIndex(), indexedRow.getRow()),
                            "update row index after temporary sort");
            return new LocalGrid(runner, columnModel, newRows, overlayModels, -1);
        }
    }

    @Override
    public Grid reorderRecords(SortingConfig sortingConfig, boolean permanent) {
        RecordSorter recordSorter = new RecordSorter(this, sortingConfig);
        PLL<Record> sorted = records()
                .values()
                .sort(recordSorter);
        if (permanent) {
            PairPLL<Long, Row> newRows = sorted
                    .flatMap(record -> CloseableIterator.wrapping(record.getRows().iterator()), "record to rows")
                    .zipWithIndex();
            // reordering records can change the number of records, if the first record does not have a non-blank value
            // in the first cell of the key column, so we are not preserving any cached count
            return new LocalGrid(runner, newRows, columnModel, overlayModels, -1);
        } else {
            PLL<IndexedRow> pll = sorted
                    .flatMap(record -> CloseableIterator.wrapping(record.getIndexedRows().iterator()),
                            "record to indexed rows");
            PairPLL<Long, IndexedRow> newRows = pll
                    .zipWithIndex()
                    .mapValues((newIndex, indexedRow) -> new IndexedRow(newIndex, indexedRow.getLogicalIndex(), indexedRow.getRow()),
                            "update row index after temporary sort");
            return new LocalGrid(runner, columnModel, newRows, overlayModels, -1);
        }
    }

    @Override
    public Grid removeRows(RowFilter filter) {
        PairPLL<Long, Row> newGrid = grid
                .values()
                .filter(tuple -> !filter.filterRow(tuple.getLogicalIndex(), tuple.getRow()))
                .zipWithIndex()
                .mapValues((index, indexedRow) -> indexedRow.getRow(), "drop old row indices before row removal");
        return new LocalGrid(runner, newGrid, columnModel, overlayModels, -1);
    }

    @Override
    public Grid removeRecords(RecordFilter filter) {
        PairPLL<Long, Row> newGrid = records()
                .values()
                .filter(record -> !filter.filterRecord(record))
                .flatMap(record -> CloseableIterator.wrapping(record.getRows().iterator()), "record to rows")
                .zipWithIndex();
        return new LocalGrid(runner, newGrid, columnModel, overlayModels, -1);
    }

    @Override
    public <T> ChangeData<T> mapRows(RowFilter filter, RowChangeDataProducer<T> rowMapper, Optional<ChangeData<T>> incompleteChangeData) {
        PairPLL<Long, IndexedRow> filteredGrid = grid
                .filter(tuple -> filter.filterRow(tuple.getValue().getLogicalIndex(), tuple.getValue().getRow()));
        PairPLL<Long, IndexedData<T>> data;
        if (incompleteChangeData.isEmpty()) {
            if (rowMapper.getBatchSize() == 1) {
                data = filteredGrid.mapValues(
                        (id, row) -> new IndexedData<>(id, rowMapper.call(row.getLogicalIndex(), row.getRow())),
                        "apply row change data producer");
            } else {
                data = filteredGrid
                        .values()
                        .batchPartitions(rowMapper.getBatchSize())
                        .flatMap(rowBatch -> applyRowChangeDataMapper(rowMapper, rowBatch), "apply row change data producer")
                        .mapToPair(indexedData -> indexedData, "bureaucratic map to pair")
                        .withPartitioner(grid.getPartitioner());
            }
        } else {
            PairPLL<Long, IndexedData<T>> incompletePLL = ((LocalChangeData<T>) incompleteChangeData.get()).getPLL();
            data = filteredGrid
                    .leftJoinOrdered(incompletePLL, Comparator.naturalOrder())
                    .batchPartitions(rowMapper.getBatchSize())
                    .flatMap(rowBatch -> applyRowChangeDataMapperWithIncompleteData(rowMapper, rowBatch),
                            "apply row change data producer to missing rows")
                    .mapToPair(indexedData -> indexedData, "bureaucratic map to pair")
                    .withPartitioner(grid.getPartitioner());
        }
        return new LocalChangeData<T>(
                runner,
                data.filter(tuple -> tuple.getValue() != null),
                grid.hasCachedPartitionSizes() ? grid.getPartitionSizes() : null,
                () -> true,
                rowMapper.getMaxConcurrency());
    }

    protected static <T> CloseableIterator<Tuple2<Long, IndexedData<T>>> applyRowChangeDataMapper(RowChangeDataProducer<T> rowMapper,
            List<IndexedRow> rowBatch) {
        List<T> changeData = rowMapper.callRowBatch(rowBatch);
        if (changeData.size() != rowBatch.size()) {
            throw new IllegalStateException(
                    String.format("Change data producer returned %d results on a batch of %d rows", changeData.size(), rowBatch.size()));
        }
        return CloseableIterator.wrapping(
                io.vavr.collection.Iterator.range(0, rowBatch.size())
                        .map(i -> {
                            long index = rowBatch.get(i).getIndex();
                            return new Tuple2<>(index, new IndexedData<>(index, changeData.get(i)));
                        }));
    }

    protected static <T> CloseableIterator<Tuple2<Long, IndexedData<T>>> applyRowChangeDataMapperWithIncompleteData(
            RowChangeDataProducer<T> rowMapper,
            List<Tuple2<Long, Tuple2<IndexedRow, IndexedData<T>>>> rowBatch) {
        List<IndexedRow> toCompute = rowBatch.stream()
                .filter(tuple -> tuple.getValue().getValue() == null || tuple.getValue().getValue().isPending())
                .map(tuple -> tuple.getValue().getKey())
                .collect(Collectors.toList());
        List<T> changeData = rowMapper.callRowBatch(toCompute);
        if (changeData.size() != toCompute.size()) {
            throw new IllegalStateException(
                    String.format("Change data producer returned %d results on a batch of %d rows", changeData.size(), toCompute.size()));
        }
        Map<Long, Optional<T>> indexedChangeData = IntStream.range(0, toCompute.size())
                .boxed()
                .collect(Collectors.toMap(i -> toCompute.get(i).getIndex(), i -> Optional.ofNullable(changeData.get(i))));
        return CloseableIterator.wrapping(io.vavr.collection.Iterator.range(0, rowBatch.size())
                .map(i -> {
                    IndexedData<T> preComputed = rowBatch.get(i).getValue().getValue();
                    long rowIndex = rowBatch.get(i).getKey();
                    if (preComputed == null || preComputed.isPending()) {
                        preComputed = new IndexedData<>(rowIndex, indexedChangeData.get(rowIndex).orElse(null));
                    }
                    return Tuple2.of(rowIndex, preComputed);
                }));
    }

    @Override
    public <T> ChangeData<T> mapRecords(RecordFilter filter,
            RecordChangeDataProducer<T> recordMapper, Optional<ChangeData<T>> incompleteChangeData) {
        PairPLL<Long, Record> filteredRecords = records().filter(tuple -> filter.filterRecord(tuple.getValue()));
        PairPLL<Long, IndexedData<T>> data;
        if (incompleteChangeData.isEmpty()) {
            if (recordMapper.getBatchSize() == 1) {
                data = filteredRecords
                        .mapValues((id, record) -> new IndexedData<>(id, recordMapper.call(record)), "apply record change data producer");
            } else {
                data = filteredRecords
                        .batchPartitions(recordMapper.getBatchSize())
                        .flatMap(batch -> applyRecordChangeDataMapper(recordMapper, batch), "apply record change data mapper")
                        .mapToPair(tuple -> tuple, "bureaucratic map to pair")
                        .withPartitioner(filteredRecords.getPartitioner());
            }
        } else {
            PairPLL<Long, IndexedData<T>> incompletePLL = ((LocalChangeData<T>) incompleteChangeData.get()).getPLL();
            data = filteredRecords
                    .leftJoinOrdered(incompletePLL, Comparator.naturalOrder())
                    .batchPartitions(recordMapper.getBatchSize())
                    .flatMap(batch -> applyRecordChangeDataMapperWithIncompleteData(recordMapper, batch), "apply record change data mapper")
                    .mapToPair(tuple -> tuple, "bureaucratic map to pair")
                    .withPartitioner(filteredRecords.getPartitioner());
        }
        return new LocalChangeData<T>(
                runner,
                data.filter(tuple -> tuple.getValue() != null),
                grid.hasCachedPartitionSizes() ? grid.getPartitionSizes() : null,
                () -> true,
                recordMapper.getMaxConcurrency());
    }

    /**
     * Only keep the first rows. Overridden for efficiency when a partitioner is known.
     * 
     * @param rowLimit
     *            the number of rows to keep
     * @return the limited grid
     */
    @Override
    public Grid limitRows(long rowLimit) {
        return removeRows(RowFilter.limitFilter(rowLimit));
    }

    /**
     * Drop the first rows. Overridden for efficiency when partition sizes are known.
     * 
     * @param rowsToDrop
     *            the number of rows to drop
     * @return the grid consisting of the last rows
     */
    @Override
    public Grid dropRows(long rowsToDrop) {
        // We force the computation of partition sizes
        // because they would be computed anyway to re-index
        // the rows after the drop.
        grid.getPartitionSizes();
        PairPLL<Long, IndexedRow> dropped = grid.dropFirstElements(rowsToDrop);
        Optional<Partitioner<Long>> partitioner = dropped.getPartitioner();
        if (partitioner.isPresent() && partitioner.get() instanceof LongRangePartitioner) {
            partitioner = partitioner.map(p -> ((LongRangePartitioner) p).shiftKeys(-rowsToDrop));
        }
        PairPLL<Long, IndexedRow> shifted = dropped
                .mapToPair(tuple -> Tuple2.of(tuple.getKey() - rowsToDrop,
                        new IndexedRow(tuple.getKey() - rowsToDrop, tuple.getValue().getRow())),
                        "adjust row ids after dropping rows")
                .withPartitioner(partitioner);
        return new LocalGrid(runner, columnModel, shifted, overlayModels, -1);
    }

    protected static <T> CloseableIterator<Tuple2<Long, IndexedData<T>>> applyRecordChangeDataMapper(
            RecordChangeDataProducer<T> recordMapper,
            List<Tuple2<Long, Record>> recordBatch) {
        List<T> changeData = recordMapper.callRecordBatch(
                recordBatch.stream()
                        .map(Tuple2::getValue)
                        .collect(Collectors.toList()));
        if (changeData.size() != recordBatch.size()) {
            throw new IllegalStateException(String.format("Change data producer returned %d results on a batch of %d records",
                    changeData.size(), recordBatch.size()));
        }
        return CloseableIterator.wrapping(
                io.vavr.collection.Iterator.range(0, recordBatch.size())
                        .map(i -> {
                            long id = recordBatch.get(i).getKey();
                            return new Tuple2<>(id, new IndexedData<>(id, changeData.get(i)));
                        }));
    }

    protected static <T> CloseableIterator<Tuple2<Long, IndexedData<T>>> applyRecordChangeDataMapperWithIncompleteData(
            RecordChangeDataProducer<T> recordMapper,
            List<Tuple2<Long, Tuple2<Record, IndexedData<T>>>> recordBatch) {
        List<Record> toCompute = recordBatch.stream()
                .filter(tuple -> tuple.getValue().getValue() == null || tuple.getValue().getValue().isPending())
                .map(tuple -> tuple.getValue().getKey())
                .collect(Collectors.toList());
        List<T> changeData = recordMapper.callRecordBatch(toCompute);
        if (changeData.size() != toCompute.size()) {
            throw new IllegalStateException(
                    String.format("Change data producer returned %d results on a batch of %d records", changeData.size(),
                            toCompute.size()));
        }
        Map<Long, T> indexedChangeData = IntStream.range(0, toCompute.size())
                .boxed()
                .collect(Collectors.toMap(i -> toCompute.get(i).getStartRowId(), changeData::get));
        return CloseableIterator.wrapping(io.vavr.collection.Iterator.range(0, recordBatch.size())
                .map(i -> {
                    IndexedData<T> preComputed = recordBatch.get(i).getValue().getValue();
                    long rowIndex = recordBatch.get(i).getKey();
                    if (preComputed == null || preComputed.isPending()) {
                        preComputed = new IndexedData<>(rowIndex, indexedChangeData.get(rowIndex));
                    }
                    return Tuple2.of(rowIndex, preComputed);
                }));
    }

    @Override
    public <T> Grid join(ChangeData<T> changeData, RowChangeDataJoiner<T> rowJoiner,
            ColumnModel newColumnModel) {
        if (!(changeData instanceof LocalChangeData<?>)) {
            throw new IllegalArgumentException("A LocalGrid can only be joined with a LocalChangeData");
        }
        PairPLL<Long, Row> joined = grid
                .leftJoinOrdered(((LocalChangeData<T>) changeData).getPLL(), Comparator.naturalOrder())
                .mapValues((id, tuple) -> {
                    IndexedData<T> indexedData = tuple.getValue();
                    if (indexedData == null) {
                        indexedData = new IndexedData<T>(id, null);
                    }
                    return rowJoiner.call(tuple.getKey().getRow(), indexedData);
                }, "apply row change data joiner");
        return new LocalGrid(runner, joined, newColumnModel, overlayModels, rowJoiner.preservesRecordStructure() ? cachedRecordCount : -1);
    }

    @Override
    public <T> Grid join(ChangeData<T> changeData, RowChangeDataFlatJoiner<T> rowJoiner,
            ColumnModel newColumnModel) {
        if (!(changeData instanceof LocalChangeData<?>)) {
            throw new IllegalArgumentException("A LocalGrid can only be joined with a LocalChangeData");
        }
        PairPLL<Long, Row> joined = grid
                .leftJoinOrdered(((LocalChangeData<T>) changeData).getPLL(), Comparator.naturalOrder())
                .flatMap(tuple -> {
                    IndexedData<T> indexedData = tuple.getValue().getValue();
                    if (indexedData == null) {
                        indexedData = new IndexedData<T>(tuple.getKey(), null);
                    }
                    return CloseableIterator.wrapping(
                            rowJoiner.call(tuple.getValue().getKey().getRow(), indexedData).iterator());
                },
                        "apply row change data joiner")
                .zipWithIndex();
        return new LocalGrid(runner, joined, newColumnModel, overlayModels, -1);
    }

    @Override
    public <T> Grid join(ChangeData<T> changeData, RecordChangeDataJoiner<T> recordJoiner,
            ColumnModel newColumnModel) {
        if (!(changeData instanceof LocalChangeData<?>)) {
            throw new IllegalArgumentException("A LocalGrid can only be joined with a LocalChangeData");
        }
        PairPLL<Long, Tuple2<Record, IndexedData<T>>> joinedRecords = records()
                .leftJoinOrdered(((LocalChangeData<T>) changeData).getPLL(), Comparator.naturalOrder());

        if (recordJoiner.preservesRecordStructure()) {
            PairPLL<Long, Record> records = joinedRecords
                    .mapValues((recordId, tuple) -> {
                        IndexedData<T> indexedData = tuple.getValue();
                        if (indexedData == null) {
                            indexedData = new IndexedData<>(recordId, null);
                        }
                        return new Record(recordId, recordJoiner.call(tuple.getKey(), indexedData));
                    },
                            "apply record change data joiner with record boundaries preserved");
            return new LocalGrid(records, runner, newColumnModel, overlayModels, rowCount());
        } else {
            PairPLL<Long, Row> joined = joinedRecords
                    .flatMap(tuple -> {
                        IndexedData<T> indexedData = tuple.getValue().getValue();
                        if (indexedData == null) {
                            indexedData = new IndexedData<T>(tuple.getKey(), null);
                        }
                        return CloseableIterator.wrapping(
                                recordJoiner.call(tuple.getValue().getKey(), indexedData).iterator());
                    },
                            "apply record change data joiner")
                    .zipWithIndex();
            return new LocalGrid(runner, joined, newColumnModel, overlayModels, -1);
        }
    }

    @Override
    public Grid concatenate(Grid other) {
        return concatenate(Collections.singletonList(other));
    }

    @Override
    public Grid concatenate(List<Grid> otherGrids) {
        List<Grid> fullList = new ArrayList<>(otherGrids.size() + 1);
        fullList.add(this);
        fullList.addAll(otherGrids);
        return concatenateGridList(fullList);
    }

    protected static Grid concatenateGridList(List<Grid> grids) {
        if (grids.isEmpty()) {
            throw new IllegalArgumentException("Concatenating an empty list of grids");
        }
        if (!grids.stream().allMatch(grid -> grid instanceof LocalGrid)) {
            throw new IllegalArgumentException("Concatenating grids from incompatible runners");
        }

        List<LocalGrid> localGrids = grids.stream().map(grid -> (LocalGrid) grid).collect(Collectors.toList());
        ColumnModel mergedColumnModel = grids.get(0).getColumnModel();
        Map<String, OverlayModel> mergedOverlayModels = new HashMap<>();
        List<PLL<Row>> plls = new ArrayList<>(grids.size());
        long recordCount = 0L;
        for (LocalGrid grid : localGrids) {
            mergedColumnModel = mergedColumnModel.merge(grid.getColumnModel());
            mergedOverlayModels.putAll(grid.getOverlayModels());
            // compute the record count of the new grid if it can directly be inferred from the
            // existing cached counts.
            if (recordCount != -1L) {
                if (grid.cachedRecordCount == -1L) {
                    recordCount = -1L;
                } else {
                    // a record in the first grid might continue over from the first to the second grid,
                    // if the first record in the second grid does not start with a record start marker.
                    if (recordCount != 0L && grid.cachedRecordCount != 0L &&
                            !Record.isRecordStart(grid.getRow(0L), mergedColumnModel.getKeyColumnIndex())) {
                        recordCount -= 1L;
                    }
                    recordCount += grid.cachedRecordCount;
                }
            }
            PLL<Row> currentRows = grid.indexedRows().map(IndexedRow::getRow, "drop old row indices before concatenation");
            plls.add(currentRows);
        }

        PLL<Row> rows = plls.get(0).concatenate(plls.subList(1, plls.size()));
        return new LocalGrid(
                localGrids.get(0).getRunner(),
                rows.zipWithIndex(),
                mergedColumnModel,
                mergedOverlayModels, recordCount);
    }

    @Override
    public boolean isCached() {
        return constructedFromRows ? grid.isCached() : records.isCached();
    }

    @Override
    public void uncache() {
        if (constructedFromRows) {
            grid.uncache();
        } else {
            records.uncache();
        }
    }

    @Override
    public ProgressingFuture<Boolean> cacheAsync() {
        if (!smallEnoughToCacheInMemory()) {
            return ProgressingFutures.immediate(false);
        }
        if (constructedFromRows) {
            // if the grid was constructed from rows and the records were derived from that,
            // then cache the rows: the records will be directly derived from something cached, so they will be fast too
            return ProgressingFutures.transform(grid.cacheAsync(), v -> isCached(), runner.getPLLContext().getExecutorService());
        } else {
            // otherwise, do the converse
            return ProgressingFutures.transform(records.cacheAsync(), v -> isCached(), runner.getPLLContext().getExecutorService());
        }
    }

    @Override
    public boolean cache() {
        try {
            return cacheAsync().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    /**
     * Heuristic which predicts how much RAM a grid will occupy in memory after caching.
     *
     * @return whether there is enough free memory to cache this one.
     */
    protected boolean smallEnoughToCacheInMemory() {
        long rows = rowCount();
        List<ColumnMetadata> columns = columnModel.getColumns();
        long unreconciledColumns = columns.stream().filter(c -> c.getReconConfig() == null).count();
        long reconciledColumns = columns.size() - unreconciledColumns;
        long predictedMemorySize = (runner.getUnreconciledCellCost() * unreconciledColumns +
                runner.getReconciledCellCost() * reconciledColumns) * rows;
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory();
        return freeMemory > predictedMemorySize;
    }

    /**
     * Compute how many partitions and how many elements in those partitions we should process, if we want to get a
     * sample of the given size. This will generally result in using all partitions and dividing the sample size by that
     * number of partitions, but if there are many partitions we might not want to use them all (for instance, if there
     * are more partitions than the desired sample size).
     *
     * @param sampleSize
     *            the number of elements we want to process in total
     * @return a tuple, where the first component is the number of partitions and the second one is the number of
     *         elements in each of those partitions.
     */
    protected <T, U extends Serializable> PartialAggregation<U> sample(PLL<T> source, long sampleSize, U initialValue,
            BiFunction<U, T, U> fold, BiFunction<U, U, U> combine) {
        if (sampleSize <= 0) {
            return new PartialAggregation(initialValue, 0, true);
        }

        // we want to pick our samples from at most twice as many partitions as we have
        // processors, so that we have enough to max out the computing power but do not
        // open too many files either.
        // TODO this could be configurable: to get a more uniformly sampled set of rows,
        // the user could want to set that to a higher value.
        int maxPartitions = runner.defaultParallelism * 2;
        int samplingPartitions = Math.max(1, Math.min(maxPartitions, source.getPartitions().size()));
        PLL<T> samplingPLL = source;
        if (samplingPartitions < source.getPartitions().size()) {
            // spread the sampling partitions evenly in the source partitions
            int step = Math.max(source.getPartitions().size() / samplingPartitions, 1);
            List<Integer> partitionIndices = IntStream
                    .range(0, samplingPartitions).mapToObj(i -> step * i)
                    .collect(Collectors.toList());
            samplingPLL = source.retainPartitions(partitionIndices);
        }
        long partitionLimit = Math.max(sampleSize / samplingPartitions, 1L);
        return samplingPLL.limitPartitions(partitionLimit)
                .aggregate(new PartialAggregation<>(initialValue, 0, false),
                        (s, t) -> new PartialAggregation<U>(
                                fold.apply(s.getState(), t),
                                s.getProcessed() + 1,
                                s.limitReached() || s.getProcessed() + 1 == partitionLimit),
                        (s1, s2) -> new PartialAggregation<>(
                                combine.apply(s1.getState(), s2.getState()),
                                s1.getProcessed() + s2.getProcessed(),
                                s1.limitReached() || s2.limitReached()));
    }

    @Override
    public String toString() {
        return String.format("[LocalGrid:\n%s\n]", grid.toString());
    }

    public QueryTree getRowsQueryTree() {
        return grid.getQueryTree();
    }

    public QueryTree getRecordsQueryTree() {
        return records().getQueryTree();
    }

}
