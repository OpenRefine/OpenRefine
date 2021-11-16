
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

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.core.JsonProcessingException;

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
import org.openrefine.overlay.OverlayModel;
import org.openrefine.process.ProgressReporter;
import org.openrefine.sorting.RecordSorter;
import org.openrefine.sorting.RowSorter;
import org.openrefine.sorting.SortingConfig;
import org.openrefine.util.ParsingUtilities;

/**
 * A PLL-based implementation of a GridState.
 * 
 * @author Antonin Delpeuch
 *
 */
@JsonIgnoreType
public class LocalGridState implements GridState {

    protected final LocalDatamodelRunner runner;
    protected final PairPLL<Long, Row> grid;
    protected final ColumnModel columnModel;
    protected final Map<String, OverlayModel> overlayModels;

    // initialized lazily
    protected PairPLL<Long, Record> records;

    /**
     * Constructs a grid state, supplying all required fields.
     * 
     * @param runner
     * @param grid
     * @param columnModel
     * @param overlayModels
     */
    public LocalGridState(
            LocalDatamodelRunner runner,
            PairPLL<Long, Row> grid,
            ColumnModel columnModel,
            Map<String, OverlayModel> overlayModels) {
        this.runner = runner;
        this.grid = grid;
        this.columnModel = columnModel;
        this.overlayModels = overlayModels;
        this.records = null;
    }

    protected PairPLL<Long, Record> records() {
        if (records == null) {
            records = RecordPLL.groupIntoRecords(grid, columnModel.getKeyColumnIndex());
        }
        return records;
    }

    protected PLL<IndexedRow> indexedRows() {
        return grid.map(tuple -> new IndexedRow(tuple.getKey(), tuple.getValue()));
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
    public GridState withColumnModel(ColumnModel newColumnModel) {
        return new LocalGridState(runner, grid, newColumnModel, overlayModels);
    }

    @Override
    public Row getRow(long id) {
        List<Row> rows = grid.get(id);
        if (rows.size() == 0) {
            throw new IndexOutOfBoundsException(String.format("Row id %d not found", id));
        } else if (rows.size() > 1) {
            throw new IllegalStateException(String.format("Found %d rows at index %d", rows.size(), id));
        } else {
            return rows.get(0);
        }
    }

    @Override
    public List<IndexedRow> getRows(long start, int limit) {
        return grid.getRange(start, limit, Comparator.naturalOrder())
                .stream()
                .map(tuple -> new IndexedRow(tuple.getKey(), tuple.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<IndexedRow> getRows(RowFilter filter, SortingConfig sortingConfig, long start, int limit) {
        PairPLL<Long, Row> filteredRows = grid.filter(tuple -> filter.filterRow(tuple.getKey(), tuple.getValue()));
        if (SortingConfig.NO_SORTING.equals(sortingConfig)) {
            return filteredRows
                    .getRange(start, limit, Comparator.naturalOrder())
                    .stream()
                    .map(tuple -> new IndexedRow(tuple.getKey(), tuple.getValue()))
                    .collect(Collectors.toList());
        } else {
            RowSorter rowSorter = new RowSorter(this, sortingConfig);
            return filteredRows
                    .map(tuple -> new IndexedRow(tuple.getKey(), tuple.getValue()))
                    .sort(rowSorter)
                    .zipWithIndex() // this is cheap because sort loads everything in memory anyway
                    .getRange(start, limit, Comparator.naturalOrder())
                    .stream()
                    .map(tuple -> tuple.getValue())
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<IndexedRow> getRows(List<Long> rowIndices) {
        Map<Long, IndexedRow> results = grid.getByKeys(rowIndices.stream().collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(t -> t.getKey(), t -> new IndexedRow(t.getKey(), t.getValue())));
        return rowIndices.stream()
                .map(i -> results.get(i))
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<IndexedRow> iterateRows(RowFilter filter, SortingConfig sortingConfig) {
        return new Iterable<IndexedRow>() {

            @Override
            public Iterator<IndexedRow> iterator() {
                PLL<IndexedRow> filteredRows = grid
                        .filter(tuple -> filter.filterRow(tuple.getKey(), tuple.getValue()))
                        .map(tuple -> new IndexedRow(tuple.getKey(), tuple.getValue()));
                if (SortingConfig.NO_SORTING.equals(sortingConfig)) {
                    return filteredRows
                            .stream()
                            .iterator();
                } else {
                    RowSorter rowSorter = new RowSorter(LocalGridState.this, sortingConfig);
                    return filteredRows
                            .sort(rowSorter)
                            .stream()
                            .iterator();
                }
            }

        };
    }

    @Override
    public long countMatchingRows(RowFilter filter) {
        return grid.filter(tuple -> filter.filterRow(tuple.getKey(), tuple.getValue())).count();
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
                                ac.getMatched() + (filter.filterRow(tuple.getKey(), tuple.getValue()) ? 1 : 0),
                                ac.limitReached() || ac.getProcessed() + 1 == partitionLimit),
                        (ac1, ac2) -> new ApproxCount(
                                ac1.getProcessed() + ac2.getProcessed(),
                                ac1.getMatched() + ac2.getMatched(),
                                ac1.limitReached() || ac2.limitReached()));
    }

    @Override
    public List<IndexedRow> collectRows() {
        return grid.map(tuple -> new IndexedRow(tuple.getKey(), tuple.getValue())).collect();
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
    public List<Record> getRecords(long start, int limit) {
        return records()
                .getRange(start, limit, Comparator.naturalOrder())
                .stream()
                .map(tuple -> tuple.getValue())
                .collect(Collectors.toList());
    }

    @Override
    public List<Record> getRecords(RecordFilter filter, SortingConfig sortingConfig, long start, int limit) {
        PairPLL<Long, Record> records = records();
        if (!SortingConfig.NO_SORTING.equals(sortingConfig)) {
            RecordSorter recordSorter = new RecordSorter(this, sortingConfig);
            records = records()
                    .values()
                    .sort(recordSorter)
                    .zipWithIndex();
            // this zipWithIndex is efficient because sorting fetches everything in
            // memory, and it is trivial to zipWithIndex an InMemoryPLL.
        }
        return records
                .filter(tuple -> filter.filterRecord(tuple.getValue()))
                .getRange(start, limit, Comparator.naturalOrder())
                .stream()
                .map(tuple -> tuple.getValue())
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<Record> iterateRecords(RecordFilter filter, SortingConfig sortingConfig) {
        return new Iterable<Record>() {

            @Override
            public Iterator<Record> iterator() {
                PLL<Record> records = records().values();
                if (!SortingConfig.NO_SORTING.equals(sortingConfig)) {
                    RecordSorter recordSorter = new RecordSorter(LocalGridState.this, sortingConfig);
                    records = records.sort(recordSorter);
                }
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
                .map(LocalGridState::serializeIndexedRow)
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

    protected static String serializeIndexedRow(Tuple2<Long, Row> tuple) {
        try {
            return ParsingUtilities.saveWriter.writeValueAsString(new IndexedRow(tuple.getKey(), tuple.getValue()));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <T extends Serializable> T aggregateRows(RowAggregator<T> aggregator, T initialState) {
        return grid.aggregate(initialState,
                (s, t) -> aggregator.withRow(s, t.getKey(), t.getValue()),
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
                .aggregate(initialPartialState,
                        (s, t) -> new PartialAggregation<T>(
                                aggregator.withRow(s.getState(), t.getKey(), t.getValue()),
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
    public GridState withOverlayModels(Map<String, OverlayModel> newOverlayModels) {
        return new LocalGridState(runner, grid, columnModel, newOverlayModels);
    }

    @Override
    public GridState mapRows(RowMapper mapper, ColumnModel newColumnModel) {
        PairPLL<Long, Row> newGrid = grid.mapValues((i, r) -> mapper.call(i, r));
        return new LocalGridState(runner, newGrid, newColumnModel, overlayModels);
    }

    @Override
    public GridState flatMapRows(RowFlatMapper mapper, ColumnModel newColumnModel) {
        PairPLL<Long, Row> newGrid = grid.flatMap(tuple -> mapper.call(tuple.getKey(), tuple.getValue()).stream()).zipWithIndex();
        return new LocalGridState(runner, newGrid, newColumnModel, overlayModels);
    }

    @Override
    public <S extends Serializable> GridState mapRows(RowScanMapper<S> mapper, ColumnModel newColumnModel) {
        PLL<Tuple2<Long, Row>> newGrid = grid.scanMap(
                mapper.unit(),
                tuple -> mapper.feed(tuple.getKey(), tuple.getValue()),
                (s1, s2) -> mapper.combine(s1, s2),
                (s, tuple) -> Tuple2.of(tuple.getKey(), mapper.map(s, tuple.getKey(), tuple.getValue())));
        PairPLL<Long, Row> paired = new PairPLL<>(newGrid, grid.getPartitioner());

        return new LocalGridState(runner, paired, newColumnModel, overlayModels);
    }

    @Override
    public GridState mapRecords(RecordMapper mapper, ColumnModel newColumnModel) {
        PairPLL<Long, Row> grid = records()
                .values()
                .flatMap(record -> mapper.call(record).stream())
                .zipWithIndex();
        return new LocalGridState(runner, grid, newColumnModel, overlayModels);
    }

    @Override
    public GridState reorderRows(SortingConfig sortingConfig) {
        RowSorter rowSorter = new RowSorter(this, sortingConfig);
        PairPLL<Long, Row> newRows = indexedRows()
                .sort(rowSorter)
                .map(IndexedRow::getRow)
                .zipWithIndex();

        return new LocalGridState(runner, newRows, columnModel, overlayModels);
    }

    @Override
    public GridState reorderRecords(SortingConfig sortingConfig) {
        RecordSorter recordSorter = new RecordSorter(this, sortingConfig);
        PairPLL<Long, Row> newRows = records()
                .values()
                .sort(recordSorter)
                .flatMap(record -> record.getRows().stream())
                .zipWithIndex();

        return new LocalGridState(runner, newRows, columnModel, overlayModels);
    }

    @Override
    public GridState removeRows(RowFilter filter) {
        PairPLL<Long, Row> newGrid = grid
                .filter(tuple -> !filter.filterRow(tuple.getKey(), tuple.getValue()))
                .values()
                .zipWithIndex();
        return new LocalGridState(runner, newGrid, columnModel, overlayModels);
    }

    @Override
    public GridState removeRecords(RecordFilter filter) {
        PairPLL<Long, Row> newGrid = records()
                .values()
                .filter(record -> !filter.filterRecord(record))
                .flatMap(record -> record.getRows().stream())
                .zipWithIndex();
        return new LocalGridState(runner, newGrid, columnModel, overlayModels);
    }

    @Override
    public <T> ChangeData<T> mapRows(RowFilter filter, RowChangeDataProducer<T> rowMapper) {
        PairPLL<Long, Row> filteredGrid = grid.filter(tuple -> filter.filterRow(tuple.getKey(), tuple.getValue()));
        PairPLL<Long, T> data;
        if (rowMapper.getBatchSize() == 1) {
            data = filteredGrid.mapValues(
                    (id, row) -> rowMapper.call(id, row));
        } else {
            data = filteredGrid
                    .batchPartitions(rowMapper.getBatchSize())
                    .flatMap(rowBatch -> applyRowChangeDataMapper(rowMapper, rowBatch))
                    .mapToPair(indexedData -> indexedData)
                    .withPartitioner(grid.getPartitioner());
        }
        return new LocalChangeData<T>(
                runner,
                data.filter(tuple -> tuple.getValue() != null),
                grid.hasCachedPartitionSizes() ? grid.getPartitionSizes() : null);
    }

    protected static <T> Stream<Tuple2<Long, T>> applyRowChangeDataMapper(RowChangeDataProducer<T> rowMapper,
            List<Tuple2<Long, Row>> rowBatch) {
        List<T> changeData = rowMapper.callRowBatch(
                rowBatch.stream()
                        .map(tuple -> new IndexedRow(tuple.getKey(), tuple.getValue()))
                        .collect(Collectors.toList()));
        if (changeData.size() != rowBatch.size()) {
            throw new IllegalStateException(
                    String.format("Change data producer returned %d results on a batch of %d rows", changeData.size(), rowBatch.size()));
        }
        return IntStream.range(0, rowBatch.size())
                .mapToObj(i -> new Tuple2<Long, T>(rowBatch.get(i).getKey(), changeData.get(i)));
    }

    @Override
    public <T> ChangeData<T> mapRecords(RecordFilter filter,
            RecordChangeDataProducer<T> recordMapper) {
        PairPLL<Long, Record> filteredRecords = records().filter(tuple -> filter.filterRecord(tuple.getValue()));
        PairPLL<Long, T> data;
        if (recordMapper.getBatchSize() == 1) {
            data = filteredRecords
                    .mapValues((id, record) -> recordMapper.call(record));
        } else {
            data = filteredRecords
                    .batchPartitions(recordMapper.getBatchSize())
                    .flatMap(batch -> applyRecordChangeDataMapper(recordMapper, batch))
                    .mapToPair(tuple -> tuple)
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
    public GridState limitRows(long rowLimit) {
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
    public GridState dropRows(long rowsToDrop) {
        // We force the computation of partition sizes
        // because they would be computed anyway to re-index
        // the rows after the drop.
        grid.getPartitionSizes();
        PairPLL<Long, Row> dropped = grid.dropFirstElements(rowsToDrop);
        Optional<Partitioner<Long>> partitioner = dropped.getPartitioner();
        if (partitioner.isPresent() && partitioner.get() instanceof LongRangePartitioner) {
            partitioner = partitioner.map(p -> ((LongRangePartitioner) p).shiftKeys(-rowsToDrop));
        }
        PairPLL<Long, Row> shifted = dropped.mapToPair(tuple -> Tuple2.of(tuple.getKey() - rowsToDrop, tuple.getValue()))
                .withPartitioner(partitioner);
        return new LocalGridState(runner, shifted, columnModel, overlayModels);
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
    public <T> GridState join(ChangeData<T> changeData, RowChangeDataJoiner<T> rowJoiner,
            ColumnModel newColumnModel) {
        if (!(changeData instanceof LocalChangeData<?>)) {
            throw new IllegalArgumentException("A LocalGridState can only be joined with a LocalChangeData");
        }
        PairPLL<Long, Row> joined = grid
                .outerJoinOrdered(((LocalChangeData<T>) changeData).getPLL(), Comparator.naturalOrder())
                .mapValues((id, tuple) -> rowJoiner.call(id, tuple.getKey(), tuple.getValue()));
        return new LocalGridState(runner, joined, newColumnModel, overlayModels);
    }

    @Override
    public <T> GridState join(ChangeData<T> changeData, RowChangeDataFlatJoiner<T> rowJoiner,
            ColumnModel newColumnModel) {
        if (!(changeData instanceof LocalChangeData<?>)) {
            throw new IllegalArgumentException("A LocalGridState can only be joined with a LocalChangeData");
        }
        PairPLL<Long, Row> joined = grid
                .outerJoinOrdered(((LocalChangeData<T>) changeData).getPLL(), Comparator.naturalOrder())
                .flatMap(tuple -> rowJoiner.call(tuple.getKey(), tuple.getValue().getKey(), tuple.getValue().getValue()).stream())
                .zipWithIndex();
        return new LocalGridState(runner, joined, newColumnModel, overlayModels);
    }

    @Override
    public <T> GridState join(ChangeData<T> changeData, RecordChangeDataJoiner<T> recordJoiner,
            ColumnModel newColumnModel) {
        if (!(changeData instanceof LocalChangeData<?>)) {
            throw new IllegalArgumentException("A LocalGridState can only be joined with a LocalChangeData");
        }
        PairPLL<Long, Row> joined = records()
                .outerJoinOrdered(((LocalChangeData<T>) changeData).getPLL(), Comparator.naturalOrder())
                .flatMap(tuple -> recordJoiner.call(tuple.getValue().getKey(), tuple.getValue().getValue()).stream())
                .zipWithIndex();
        return new LocalGridState(runner, joined, newColumnModel, overlayModels);
    }

    @Override
    public GridState concatenate(GridState other) {
        if (!(other instanceof LocalGridState)) {
            throw new IllegalArgumentException("Concatenating grid states from incompatible runners");
        }
        LocalGridState otherLocal = (LocalGridState) other;
        ColumnModel merged = columnModel.merge(other.getColumnModel());
        Map<String, OverlayModel> mergedOverlayModels = new HashMap<>(other.getOverlayModels());
        mergedOverlayModels.putAll(overlayModels);
        return new LocalGridState(
                runner,
                grid.values().concatenate(otherLocal.grid.values()).zipWithIndex(),
                merged,
                mergedOverlayModels);
    }

    @Override
    public boolean isCached() {
        return grid.isCached();
    }

    @Override
    public void uncache() {
        grid.uncache();
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
        grid.cache(progressReporter);
        return grid.isCached();
    }

}
