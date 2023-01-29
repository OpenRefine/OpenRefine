
package org.openrefine.model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.collections.IteratorUtils;

import org.openrefine.browsing.facets.RecordAggregator;
import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.RecordChangeDataJoiner;
import org.openrefine.model.changes.RecordChangeDataProducer;
import org.openrefine.model.changes.RowChangeDataFlatJoiner;
import org.openrefine.model.changes.RowChangeDataJoiner;
import org.openrefine.model.changes.RowChangeDataProducer;
import org.openrefine.model.local.PLL;
import org.openrefine.model.local.PairPLL;
import org.openrefine.model.local.RecordPLL;
import org.openrefine.model.local.Tuple2;
import org.openrefine.model.local.partitioning.LongRangePartitioner;
import org.openrefine.model.local.partitioning.Partitioner;
import org.openrefine.model.local.util.QueryTree;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.process.ProgressReporter;
import org.openrefine.sorting.RecordSorter;
import org.openrefine.sorting.RowSorter;
import org.openrefine.sorting.SortingConfig;
import org.openrefine.util.ParsingUtilities;

/**
 * A PLL-based implementation of a Grid.
 * 
 * @author Antonin Delpeuch
 *
 */
@JsonIgnoreType
public class LocalGrid implements Grid {

    protected final LocalDatamodelRunner runner;
    protected final PairPLL<Long, IndexedRow> grid;
    protected final ColumnModel columnModel;
    protected final Map<String, OverlayModel> overlayModels;

    // flag to indicate whether the grid was constructed from a PLL of rows or records.
    // This helps determine which one should be cached, to make sure both rows and record
    // access is fast while only caching a single PLL.
    protected final boolean constructedFromRows;

    // initialized lazily
    protected PairPLL<Long, Record> records;

    /**
     * Constructs a grid, supplying all required fields.
     * 
     * @param runner
     * @param columnModel
     * @param grid
     * @param overlayModels
     */
    public LocalGrid(
            LocalDatamodelRunner runner,
            ColumnModel columnModel,
            PairPLL<Long, IndexedRow> grid,
            Map<String, OverlayModel> overlayModels) {
        this.runner = runner;
        this.grid = grid;
        this.columnModel = columnModel;
        this.overlayModels = overlayModels;
        this.records = null;
        this.constructedFromRows = true;
        if (grid.getPartitioner().isEmpty()) {
            throw new IllegalArgumentException("No partitioner supplied for the rows PLL");
        }
    }

    /**
     * Convenience constructor to construct a grid from a PLL of a slightly different type.
     *
     * @param runner
     * @param grid
     * @param columnModel
     * @param overlayModels
     */
    public LocalGrid(
            LocalDatamodelRunner runner,
            PairPLL<Long, Row> grid,
            ColumnModel columnModel,
            Map<String, OverlayModel> overlayModels) {
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
            LocalDatamodelRunner runner,
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
                .flatMap(record -> StreamSupport.stream(record.getIndexedRows().spliterator(), false), "flatten records to rows")
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
    public DatamodelRunner getDatamodelRunner() {
        return runner;
    }

    @Override
    public ColumnModel getColumnModel() {
        return columnModel;
    }

    @Override
    public Grid withColumnModel(ColumnModel newColumnModel) {
        if (constructedFromRows) {
            return new LocalGrid(runner, newColumnModel, grid, overlayModels);
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
        Map<Long, IndexedRow> results = grid.getByKeys(rowIndices.stream().collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(t -> t.getKey(), t -> t.getValue()));
        return rowIndices.stream()
                .map(i -> results.get(i))
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<IndexedRow> iterateRows(RowFilter filter) {
        return new Iterable<IndexedRow>() {

            @Override
            public Iterator<IndexedRow> iterator() {
                PLL<IndexedRow> filteredRows = grid
                        .filter(tuple -> filter.filterRow(tuple.getValue().getLogicalIndex(), tuple.getValue().getRow()))
                        .values();
                return filteredRows
                        .stream()
                        .iterator();
            }

        };
    }

    @Override
    public long countMatchingRows(RowFilter filter) {
        return grid.filter(tuple -> filter.filterRow(tuple.getValue().getLogicalIndex(), tuple.getValue().getRow())).count();
    }

    @Override
    public ApproxCount countMatchingRowsApprox(RowFilter filter, long limit) {
        long partitionLimit = limit / grid.numPartitions();
        ApproxCount initialState = new ApproxCount(0, 0, partitionLimit == 0);
        return grid
                .limitPartitions(partitionLimit)
                .aggregate(initialState,
                        (ac, tuple) -> new ApproxCount(
                                ac.getProcessed() + 1,
                                ac.getMatched() + (filter.filterRow(tuple.getValue().getLogicalIndex(), tuple.getValue().getRow()) ? 1 : 0),
                                ac.limitReached() || ac.getProcessed() + 1 == partitionLimit),
                        (ac1, ac2) -> new ApproxCount(
                                ac1.getProcessed() + ac2.getProcessed(),
                                ac1.getMatched() + ac2.getMatched(),
                                ac1.limitReached() || ac2.limitReached()));
    }

    @Override
    public List<IndexedRow> collectRows() {
        return grid.values().collect();
    }

    @Override
    public Record getRecord(long id) {
        List<Record> rows = records().get(id);
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
    public Iterable<Record> iterateRecords(RecordFilter filter) {
        return new Iterable<Record>() {

            @Override
            public Iterator<Record> iterator() {
                PLL<Record> records = records().values();
                return records
                        .filter(tuple -> filter.filterRecord(tuple))
                        .stream()
                        .iterator();
            }

        };
    }

    @Override
    public long countMatchingRecords(RecordFilter filter) {
        return records().filter(tuple -> filter.filterRecord(tuple.getValue())).count();
    }

    @Override
    public ApproxCount countMatchingRecordsApprox(RecordFilter filter, long limit) {
        PairPLL<Long, Record> records = records();
        long partitionLimit = limit / records.numPartitions();
        ApproxCount initialState = new ApproxCount(0, 0, partitionLimit == 0);
        return records
                .limitPartitions(partitionLimit)
                .aggregate(initialState,
                        (ac, tuple) -> new ApproxCount(
                                ac.getProcessed() + 1,
                                ac.getMatched() + (filter.filterRecord(tuple.getValue()) ? 1 : 0),
                                ac.limitReached() || (ac.getProcessed() + 1 == partitionLimit)),
                        (ac1, ac2) -> new ApproxCount(
                                ac1.getProcessed() + ac2.getProcessed(),
                                ac1.getMatched() + ac2.getMatched(),
                                ac1.limitReached() || ac2.limitReached()));
    }

    @Override
    public List<Record> collectRecords() {
        return records().values().collect();
    }

    @Override
    public long rowCount() {
        return grid.count();
    }

    @Override
    public long recordCount() {
        return records().count();
    }

    @Override
    public Map<String, OverlayModel> getOverlayModels() {
        return overlayModels;
    }

    @Override
    public void saveToFile(File file, ProgressReporter progressReporter) throws IOException, InterruptedException {
        saveToFile(file, Optional.ofNullable(progressReporter));
    }

    @Override
    public void saveToFile(File file) throws IOException {
        try {
            saveToFile(file, Optional.empty());
        } catch (InterruptedException e) {
            throw new IOException("The operation was interrupted", e);
        }
    }

    protected void saveToFile(File file, Optional<ProgressReporter> progressReporter) throws IOException, InterruptedException {
        File metadataFile = new File(file, METADATA_PATH);
        File gridFile = new File(file, GRID_PATH);

        grid
                .values()
                .map(LocalGrid::serializeIndexedRow, "serialize indexed row")
                .saveAsTextFile(gridFile.getAbsolutePath(), progressReporter);

        ParsingUtilities.saveWriter.writeValue(metadataFile, getMetadata());
    }

    protected Metadata getMetadata() {
        Metadata metadata = new Metadata();
        metadata.columnModel = columnModel;
        metadata.overlayModels = overlayModels;
        metadata.rowCount = grid.hasCachedPartitionSizes() ? grid.count() : -1;
        metadata.recordCount = records != null && records.hasCachedPartitionSizes() ? records.count() : -1;
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
                        (s1, s2) -> aggregator.sum(s1, s2));
    }

    @Override
    public <T extends Serializable> T aggregateRecords(RecordAggregator<T> aggregator, T initialState) {
        return records().aggregate(initialState,
                (s, t) -> aggregator.withRecord(s, t.getValue()),
                (s1, s2) -> aggregator.sum(s1, s2));
    }

    @Override
    public <T extends Serializable> PartialAggregation<T> aggregateRowsApprox(RowAggregator<T> aggregator, T initialState, long maxRows) {
        long partitionLimit = maxRows / grid.numPartitions();
        PartialAggregation<T> initialPartialState = new PartialAggregation<T>(initialState, 0, partitionLimit == 0);
        return grid
                .limitPartitions(partitionLimit)
                .values()
                .aggregate(initialPartialState,
                        (s, t) -> new PartialAggregation<T>(
                                aggregator.withRow(s.getState(), t.getLogicalIndex(), t.getRow()),
                                s.getProcessed() + 1,
                                s.limitReached() || s.getProcessed() + 1 == partitionLimit),
                        (s1, s2) -> new PartialAggregation<T>(
                                aggregator.sum(s1.getState(), s2.getState()),
                                s1.getProcessed() + s2.getProcessed(),
                                s1.limitReached() || s2.limitReached()));
    }

    @Override
    public <T extends Serializable> PartialAggregation<T> aggregateRecordsApprox(RecordAggregator<T> aggregator, T initialState,
            long maxRecords) {
        long partitionLimit = maxRecords / grid.numPartitions();
        PartialAggregation<T> initialPartialState = new PartialAggregation<T>(initialState, 0, partitionLimit == 0);
        return records()
                .limitPartitions(partitionLimit)
                .aggregate(initialPartialState,
                        (s, t) -> new PartialAggregation<T>(
                                aggregator.withRecord(s.getState(), t.getValue()),
                                s.getProcessed() + 1,
                                s.limitReached() || s.getProcessed() + 1 == partitionLimit),
                        (s1, s2) -> new PartialAggregation<T>(
                                aggregator.sum(s1.getState(), s2.getState()),
                                s1.getProcessed() + s2.getProcessed(),
                                s1.limitReached() || s2.limitReached()));
    }

    @Override
    public Grid withOverlayModels(Map<String, OverlayModel> newOverlayModels) {
        if (constructedFromRows) {
            return new LocalGrid(runner, columnModel, grid, newOverlayModels);
        } else {
            return new LocalGrid(records, runner, columnModel, newOverlayModels, grid.count());
        }
    }

    @Override
    public Grid mapRows(RowMapper mapper, ColumnModel newColumnModel) {
        PairPLL<Long, Row> newGrid = grid.mapValues((i, r) -> mapper.call(r.getLogicalIndex(), r.getRow()), "apply RowMapper");
        return new LocalGrid(runner, newGrid, newColumnModel, overlayModels);
    }

    @Override
    public Grid flatMapRows(RowFlatMapper mapper, ColumnModel newColumnModel) {
        PairPLL<Long, Row> newGrid = grid.values()
                .flatMap(tuple -> mapper.call(tuple.getLogicalIndex(), tuple.getRow()).stream(), "apply FlatRowMapper")
                .zipWithIndex();
        return new LocalGrid(runner, newGrid, newColumnModel, overlayModels);
    }

    @Override
    public <S extends Serializable> Grid mapRows(RowScanMapper<S> mapper, ColumnModel newColumnModel) {
        PLL<Tuple2<Long, Row>> newGrid = grid.scanMap(
                mapper.unit(),
                tuple -> mapper.feed(tuple.getValue().getLogicalIndex(), tuple.getValue().getRow()),
                (s1, s2) -> mapper.combine(s1, s2),
                (s, tuple) -> Tuple2.of(tuple.getKey(), mapper.map(s, tuple.getValue().getLogicalIndex(), tuple.getValue().getRow())));
        PairPLL<Long, Row> paired = new PairPLL<>(newGrid, grid.getPartitioner());

        return new LocalGrid(runner, paired, newColumnModel, overlayModels);
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
                    .flatMap(record -> mapper.call(record).stream(), "apply RecordMapper, not preserving records")
                    .zipWithIndex();
            return new LocalGrid(runner, grid, newColumnModel, overlayModels);
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
            return new LocalGrid(runner, newRows, columnModel, overlayModels);
        } else {
            PairPLL<Long, IndexedRow> newRows = indexedRows()
                    .sort(rowSorter)
                    .zipWithIndex()
                    .mapValues((newIndex, indexedRow) -> new IndexedRow(newIndex, indexedRow.getLogicalIndex(), indexedRow.getRow()),
                            "update row index after temporary sort");
            return new LocalGrid(runner, columnModel, newRows, overlayModels);
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
                    .flatMap(record -> record.getRows().stream(), "record to rows")
                    .zipWithIndex();
            return new LocalGrid(runner, newRows, columnModel, overlayModels);
        } else {
            PLL<IndexedRow> pll = sorted
                    .flatMap(record -> IteratorUtils.toList(record.getIndexedRows().iterator()).stream(),
                            "record to indexed rows");
            PairPLL<Long, IndexedRow> newRows = pll
                    .zipWithIndex()
                    .mapValues((newIndex, indexedRow) -> new IndexedRow(newIndex, indexedRow.getLogicalIndex(), indexedRow.getRow()),
                            "update row index after temporary sort");
            return new LocalGrid(runner, columnModel, newRows, overlayModels);
        }
    }

    @Override
    public Grid removeRows(RowFilter filter) {
        PairPLL<Long, Row> newGrid = grid
                .values()
                .filter(tuple -> !filter.filterRow(tuple.getLogicalIndex(), tuple.getRow()))
                .zipWithIndex()
                .mapValues((index, indexedRow) -> indexedRow.getRow(), "drop old row indices before row removal");
        return new LocalGrid(runner, newGrid, columnModel, overlayModels);
    }

    @Override
    public Grid removeRecords(RecordFilter filter) {
        PairPLL<Long, Row> newGrid = records()
                .values()
                .filter(record -> !filter.filterRecord(record))
                .flatMap(record -> record.getRows().stream(), "record to rows")
                .zipWithIndex();
        return new LocalGrid(runner, newGrid, columnModel, overlayModels);
    }

    @Override
    public <T> ChangeData<T> mapRows(RowFilter filter, RowChangeDataProducer<T> rowMapper) {
        PairPLL<Long, IndexedRow> filteredGrid = grid
                .filter(tuple -> filter.filterRow(tuple.getValue().getLogicalIndex(), tuple.getValue().getRow()));
        PairPLL<Long, T> data;
        if (rowMapper.getBatchSize() == 1) {
            data = filteredGrid.mapValues(
                    (id, row) -> rowMapper.call(row.getLogicalIndex(), row.getRow()), "apply row change data producer");
        } else {
            data = filteredGrid
                    .values()
                    .batchPartitions(rowMapper.getBatchSize())
                    .flatMap(rowBatch -> applyRowChangeDataMapper(rowMapper, rowBatch), "apply row change data producer")
                    .mapToPair(indexedData -> indexedData, "bureaucratic map to pair")
                    .withPartitioner(grid.getPartitioner());
        }
        return new LocalChangeData<T>(
                runner,
                data.filter(tuple -> tuple.getValue() != null),
                grid.hasCachedPartitionSizes() ? grid.getPartitionSizes() : null);
    }

    protected static <T> Stream<Tuple2<Long, T>> applyRowChangeDataMapper(RowChangeDataProducer<T> rowMapper,
            List<IndexedRow> rowBatch) {
        List<T> changeData = rowMapper.callRowBatch(rowBatch);
        if (changeData.size() != rowBatch.size()) {
            throw new IllegalStateException(
                    String.format("Change data producer returned %d results on a batch of %d rows", changeData.size(), rowBatch.size()));
        }
        return IntStream.range(0, rowBatch.size())
                .mapToObj(i -> new Tuple2<Long, T>(rowBatch.get(i).getIndex(), changeData.get(i)));
    }

    @Override
    public <T> ChangeData<T> mapRecords(RecordFilter filter,
            RecordChangeDataProducer<T> recordMapper) {
        PairPLL<Long, Record> filteredRecords = records().filter(tuple -> filter.filterRecord(tuple.getValue()));
        PairPLL<Long, T> data;
        if (recordMapper.getBatchSize() == 1) {
            data = filteredRecords
                    .mapValues((id, record) -> recordMapper.call(record), "apply record change data producer");
        } else {
            data = filteredRecords
                    .batchPartitions(recordMapper.getBatchSize())
                    .flatMap(batch -> applyRecordChangeDataMapper(recordMapper, batch), "apply record change data mapper")
                    .mapToPair(tuple -> tuple, "bureaucratic map to pair")
                    .withPartitioner(filteredRecords.getPartitioner());
        }
        return new LocalChangeData<T>(
                runner,
                data.filter(tuple -> tuple.getValue() != null),
                grid.hasCachedPartitionSizes() ? grid.getPartitionSizes() : null);
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
        return new LocalGrid(runner, columnModel, shifted, overlayModels);
    }

    protected static <T> Stream<Tuple2<Long, T>> applyRecordChangeDataMapper(
            RecordChangeDataProducer<T> recordMapper,
            List<Tuple2<Long, Record>> recordBatch) {
        List<T> changeData = recordMapper.callRecordBatch(
                recordBatch.stream()
                        .map(tuple -> tuple.getValue())
                        .collect(Collectors.toList()));
        if (changeData.size() != recordBatch.size()) {
            throw new IllegalStateException(String.format("Change data producer returned %d results on a batch of %d records",
                    changeData.size(), recordBatch.size()));
        }
        return IntStream.range(0, recordBatch.size())
                .mapToObj(i -> new Tuple2<Long, T>(recordBatch.get(i).getKey(), changeData.get(i)));
    }

    @Override
    public <T> Grid join(ChangeData<T> changeData, RowChangeDataJoiner<T> rowJoiner,
            ColumnModel newColumnModel) {
        if (!(changeData instanceof LocalChangeData<?>)) {
            throw new IllegalArgumentException("A LocalGrid can only be joined with a LocalChangeData");
        }
        PairPLL<Long, Row> joined = grid
                .outerJoinOrdered(((LocalChangeData<T>) changeData).getPLL(), Comparator.naturalOrder())
                .mapValues((id, tuple) -> rowJoiner.call(id, tuple.getKey().getRow(), tuple.getValue()), "apply row change data joiner");
        return new LocalGrid(runner, joined, newColumnModel, overlayModels);
    }

    @Override
    public <T> Grid join(ChangeData<T> changeData, RowChangeDataFlatJoiner<T> rowJoiner,
            ColumnModel newColumnModel) {
        if (!(changeData instanceof LocalChangeData<?>)) {
            throw new IllegalArgumentException("A LocalGrid can only be joined with a LocalChangeData");
        }
        PairPLL<Long, Row> joined = grid
                .outerJoinOrdered(((LocalChangeData<T>) changeData).getPLL(), Comparator.naturalOrder())
                .flatMap(tuple -> rowJoiner.call(tuple.getKey(), tuple.getValue().getKey().getRow(), tuple.getValue().getValue()).stream(),
                        "apply row change data joiner")
                .zipWithIndex();
        return new LocalGrid(runner, joined, newColumnModel, overlayModels);
    }

    @Override
    public <T> Grid join(ChangeData<T> changeData, RecordChangeDataJoiner<T> recordJoiner,
            ColumnModel newColumnModel) {
        // TODO return records if the recordJoiner preserves their structure
        if (!(changeData instanceof LocalChangeData<?>)) {
            throw new IllegalArgumentException("A LocalGrid can only be joined with a LocalChangeData");
        }
        PairPLL<Long, Row> joined = records()
                .outerJoinOrdered(((LocalChangeData<T>) changeData).getPLL(), Comparator.naturalOrder())
                .flatMap(tuple -> recordJoiner.call(tuple.getValue().getKey(), tuple.getValue().getValue()).stream(),
                        "apply record change data joiner")
                .zipWithIndex();
        return new LocalGrid(runner, joined, newColumnModel, overlayModels);
    }

    @Override
    public Grid concatenate(Grid other) {
        if (!(other instanceof LocalGrid)) {
            throw new IllegalArgumentException("Concatenating grids from incompatible runners");
        }
        LocalGrid otherLocal = (LocalGrid) other;
        ColumnModel merged = columnModel.merge(other.getColumnModel());
        Map<String, OverlayModel> mergedOverlayModels = new HashMap<>(other.getOverlayModels());
        mergedOverlayModels.putAll(overlayModels);
        return new LocalGrid(
                runner,
                grid.values()
                        .map(IndexedRow::getRow, "drop old row indices")
                        .concatenate(otherLocal.grid.values().map(IndexedRow::getRow, "drop old row indices"))
                        .zipWithIndex(),
                merged,
                mergedOverlayModels);
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
    public boolean cache(ProgressReporter progressReporter) {
        return cache(Optional.of(progressReporter));
    }

    @Override
    public boolean cache() {
        return cache(Optional.empty());
    }

    protected boolean cache(Optional<ProgressReporter> progressReporter) {
        if (!smallEnoughToCacheInMemory()) {
            return false;
        }
        if (constructedFromRows) {
            // if the grid was constructed from rows and the records were derived from that,
            // then cache the rows: the records will be directly derived from something cached, so they will be fast too
            grid.cache(progressReporter);
            return grid.isCached();
        } else {
            // otherwise, do the converse
            records.cache(progressReporter);
            return records.isCached();
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
