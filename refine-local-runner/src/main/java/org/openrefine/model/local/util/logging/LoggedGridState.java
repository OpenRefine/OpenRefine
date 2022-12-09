
package org.openrefine.model.local.util.logging;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.openrefine.browsing.facets.RecordAggregator;
import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.model.*;
import org.openrefine.model.Record;
import org.openrefine.model.changes.*;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.process.ProgressReporter;
import org.openrefine.sorting.SortingConfig;

/**
 * A grid state which wraps another one, adding logging to keep track of the timing of each operation.
 */
public class LoggedGridState implements GridState {

    protected final GridState grid;
    protected final LoggedDatamodelRunner runner;

    public LoggedGridState(LoggedDatamodelRunner runner, GridState grid) {
        this.grid = grid;
        this.runner = runner;
    }

    protected GridState wrap(GridState grid) {
        return new LoggedGridState(runner, grid);
    }

    protected <T> T exec(String name, Supplier<T> action) {
        return runner.exec(name, action);
    }

    protected void exec(String name, Runnable action) {
        runner.exec(name, action);
    }

    protected GridState wrap(String name, Supplier<GridState> action) {
        return runner.wrap(runner.exec(name, action));
    }

    protected <T> ChangeData<T> wrap(ChangeData<T> change) {
        return runner.wrap(change);
    }

    @Override
    public DatamodelRunner getDatamodelRunner() {
        return runner;
    }

    @Override
    public ColumnModel getColumnModel() {
        return grid.getColumnModel();
    }

    @Override
    public GridState withColumnModel(ColumnModel newColumnModel) {
        return wrap(grid.withColumnModel(newColumnModel));
    }

    @Override
    public Row getRow(long id) {
        return exec("getRow", () -> grid.getRow(id));
    }

    @Override
    public List<IndexedRow> getRowsAfter(long start, int limit) {
        return exec("getRowsAfter", () -> grid.getRowsAfter(start, limit));
    }

    @Override
    public List<IndexedRow> getRowsAfter(RowFilter filter, long start, int limit) {
        return exec("getRowsAfter", () -> grid.getRowsAfter(filter, start, limit));
    }

    @Override
    public List<IndexedRow> getRowsBefore(long end, int limit) {
        return exec("getRowsBefore", () -> grid.getRowsBefore(end, limit));
    }

    @Override
    public List<IndexedRow> getRowsBefore(RowFilter filter, long end, int limit) {
        return exec("getRowsBefore", () -> grid.getRowsBefore(filter, end, limit));
    }

    @Override
    public List<IndexedRow> getRows(List<Long> rowIndices) {
        return exec("getRows", () -> grid.getRows(rowIndices));
    }

    @Override
    public Iterable<IndexedRow> iterateRows(RowFilter filter) {
        return exec("iterateRows", () -> grid.iterateRows(filter));
    }

    @Override
    public long countMatchingRows(RowFilter filter) {
        return exec("countMatchingRows", () -> grid.countMatchingRows(filter));
    }

    @Override
    public ApproxCount countMatchingRowsApprox(RowFilter filter, long limit) {
        return exec("countMatchingRowsApprox", () -> grid.countMatchingRowsApprox(filter, limit));
    }

    @Override
    public List<IndexedRow> collectRows() {
        return exec("collectRows", () -> grid.collectRows());
    }

    @Override
    public Record getRecord(long id) {
        return exec("getRecord", () -> grid.getRecord(id));
    }

    @Override
    public List<Record> getRecordsAfter(long start, int limit) {
        return exec("getRecordsAfter", () -> grid.getRecordsAfter(start, limit));
    }

    @Override
    public List<Record> getRecordsAfter(RecordFilter filter, long start, int limit) {
        return exec("getRecordsAfter", () -> grid.getRecordsAfter(filter, start, limit));
    }

    @Override
    public List<Record> getRecordsBefore(long end, int limit) {
        return exec("getRecordsBefore", () -> grid.getRecordsBefore(end, limit));
    }

    @Override
    public List<Record> getRecordsBefore(RecordFilter filter, long end, int limit) {
        return exec("getRecordsBefore", () -> grid.getRecordsBefore(filter, end, limit));
    }

    @Override
    public Iterable<Record> iterateRecords(RecordFilter filter) {
        return exec("iterateRecords", () -> grid.iterateRecords(filter));
    }

    @Override
    public long countMatchingRecords(RecordFilter filter) {
        return exec("countMatchingRecords", () -> grid.countMatchingRecords(filter));
    }

    @Override
    public ApproxCount countMatchingRecordsApprox(RecordFilter filter, long limit) {
        return exec("countMatchingRecordsApprox", () -> grid.countMatchingRecordsApprox(filter, limit));
    }

    @Override
    public List<Record> collectRecords() {
        return exec("collectRecords", () -> grid.collectRecords());
    }

    @Override
    public long rowCount() {
        return exec("rowCount", () -> grid.rowCount());
    }

    @Override
    public long recordCount() {
        return exec("recordCount", () -> grid.recordCount());
    }

    @Override
    public Map<String, OverlayModel> getOverlayModels() {
        return grid.getOverlayModels();
    }

    @Override
    public void saveToFile(File file) throws IOException {
        exec("saveToFile", () -> {
            try {
                grid.saveToFile(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void saveToFile(File file, ProgressReporter progressReporter) throws IOException, InterruptedException {
        exec("saveToFile", () -> {
            try {
                grid.saveToFile(file, progressReporter);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public <T extends Serializable> T aggregateRows(RowAggregator<T> aggregator, T initialState) {
        return exec("aggregateRows", () -> grid.aggregateRows(aggregator, initialState));
    }

    @Override
    public <T extends Serializable> T aggregateRecords(RecordAggregator<T> aggregator, T initialState) {
        return exec("aggregateRecords", () -> grid.aggregateRecords(aggregator, initialState));
    }

    @Override
    public <T extends Serializable> PartialAggregation<T> aggregateRowsApprox(RowAggregator<T> aggregator, T initialState, long maxRows) {
        return exec("aggregateRowsApprox", () -> grid.aggregateRowsApprox(aggregator, initialState, maxRows));
    }

    @Override
    public <T extends Serializable> PartialAggregation<T> aggregateRecordsApprox(RecordAggregator<T> aggregator, T initialState,
            long maxRecords) {
        return exec("aggregateRecordsApprox", () -> grid.aggregateRecordsApprox(aggregator, initialState, maxRecords));
    }

    @Override
    public GridState withOverlayModels(Map<String, OverlayModel> overlayModel) {
        return wrap(grid.withOverlayModels(overlayModel));
    }

    @Override
    public GridState mapRows(RowMapper mapper, ColumnModel newColumnModel) {
        return wrap("mapRows", () -> grid.mapRows(mapper, newColumnModel));
    }

    @Override
    public GridState flatMapRows(RowFlatMapper mapper, ColumnModel newColumnModel) {
        return wrap("flatMapRows", () -> grid.flatMapRows(mapper, newColumnModel));
    }

    @Override
    public <S extends Serializable> GridState mapRows(RowScanMapper<S> mapper, ColumnModel newColumnModel) {
        return wrap("scanMapRows", () -> grid.mapRows(mapper, newColumnModel));
    }

    @Override
    public GridState mapRecords(RecordMapper mapper, ColumnModel newColumnModel) {
        return wrap("mapRecords", () -> grid.mapRecords(mapper, newColumnModel));
    }

    @Override
    public GridState reorderRows(SortingConfig sortingConfig, boolean permanent) {
        return wrap("reorderRows", () -> grid.reorderRows(sortingConfig, permanent));
    }

    @Override
    public GridState reorderRecords(SortingConfig sortingConfig, boolean permanent) {
        return wrap("reorderRecords", () -> grid.reorderRecords(sortingConfig, permanent));
    }

    @Override
    public GridState removeRows(RowFilter filter) {
        return wrap("removeRows", () -> grid.removeRows(filter));
    }

    @Override
    public GridState removeRecords(RecordFilter filter) {
        return wrap("removeRecords", () -> grid.removeRecords(filter));
    }

    @Override
    public GridState limitRows(long rowLimit) {
        return wrap("limitRows", () -> grid.limitRows(rowLimit));
    }

    @Override
    public GridState dropRows(long rowsToDrop) {
        return wrap("dropRows", () -> grid.dropRows(rowsToDrop));
    }

    @Override
    public <T> ChangeData<T> mapRows(RowFilter filter, RowChangeDataProducer<T> rowMapper) {
        return wrap(exec("mapRows", () -> grid.mapRows(filter, rowMapper)));
    }

    @Override
    public <T> ChangeData<T> mapRecords(RecordFilter filter, RecordChangeDataProducer<T> recordMapper) {
        return wrap(exec("mapRecords", () -> grid.mapRecords(filter, recordMapper)));
    }

    @Override
    public <T> GridState join(ChangeData<T> changeData, RowChangeDataJoiner<T> rowJoiner, ColumnModel newColumnModel) {
        return wrap(exec("join", () -> grid.join(((LoggedChangeData<T>) changeData).changeData, rowJoiner, newColumnModel)));
    }

    @Override
    public <T> GridState join(ChangeData<T> changeData, RowChangeDataFlatJoiner<T> rowJoiner, ColumnModel newColumnModel) {
        return wrap(exec("join", () -> grid.join(((LoggedChangeData<T>) changeData).changeData, rowJoiner, newColumnModel)));
    }

    @Override
    public <T> GridState join(ChangeData<T> changeData, RecordChangeDataJoiner<T> recordJoiner, ColumnModel newColumnModel) {
        return wrap(exec("join", () -> grid.join(((LoggedChangeData<T>) changeData).changeData, recordJoiner, newColumnModel)));
    }

    @Override
    public GridState concatenate(GridState other) {
        return wrap(grid.concatenate(((LoggedGridState) other).grid));
    }

    @Override
    public boolean isCached() {
        return grid.isCached();
    }

    @Override
    public void uncache() {
        exec("uncache", () -> grid.uncache());
    }

    @Override
    public boolean cache() {
        return exec("cache", () -> grid.cache());
    }

    @Override
    public boolean cache(ProgressReporter progressReporter) {
        return exec("cache", () -> grid.cache(progressReporter));
    }

    @Override
    public String toString() {
        return grid.toString();
    }
}
