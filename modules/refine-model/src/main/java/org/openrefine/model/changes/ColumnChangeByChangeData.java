
package org.openrefine.model.changes;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.*;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.*;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.*;
import org.openrefine.model.Record;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.operations.OnError;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrefine.overlay.OverlayModel;

/**
 * Adds a new column based on data fetched from an external process. If no new column name is supplied, then the change
 * will replace the column with the given name (merging with existing contents in rows not covered by the change data).
 * <p>
 * New recon config and stats can be supplied for the column changed or created. If a recon config and no recon stats
 * are provided, the change computes the new recon stats on the fly.
 */
public abstract class ColumnChangeByChangeData implements Change {

    private final String _changeDataId;
    private final String _newColumnName;
    private final String _columnName;
    private final EngineConfig _engineConfig;
    private final ReconConfig _reconConfig;

    @JsonCreator
    public ColumnChangeByChangeData(
            @JsonProperty("changeDataId") String changeDataId,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("newColumnName") String newColumnName,
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("reconConfig") ReconConfig reconConfig) {
        _changeDataId = changeDataId;
        _newColumnName = newColumnName;
        _columnName = columnName;
        _engineConfig = engineConfig;
        _reconConfig = reconConfig;
    }

    @JsonProperty("changeDataId")
    public String getChangeDataId() {
        return _changeDataId;
    }

    @JsonProperty("newColumnName")
    public String getNewColumnName() {
        return _newColumnName;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @JsonProperty("engineConfig")
    public EngineConfig getEngineConfig() {
        return _engineConfig;
    }

    @JsonProperty("reconConfig")
    public ReconConfig getReconConfig() {
        return _reconConfig;
    }

    @Override
    public ChangeResult apply(Grid projectState, ChangeContext context) throws DoesNotApplyException {
        ColumnModel columnModel = projectState.getColumnModel();
        int baseColumnIndex = columnModel.getColumnIndexByName(_columnName);
        if (baseColumnIndex == -1) {
            throw new Change.DoesNotApplyException(String.format("Column '{}' not found", _columnName));
        }
        int newColumnIndex = baseColumnIndex;
        if (_newColumnName != null) {
            ColumnMetadata column = new ColumnMetadata(_newColumnName)
                    .withReconConfig(_reconConfig);
            newColumnIndex = baseColumnIndex + 1;
            try {
                columnModel = projectState.getColumnModel().insertColumn(newColumnIndex, column);
            } catch (ModelException e) {
                throw new Change.DoesNotApplyException(
                        String.format("A column with name '{}' cannot be added as the name conflicts with an existing column",
                                _columnName));
            }
        } else if (_reconConfig != null) {
            columnModel = columnModel
                    .withReconConfig(baseColumnIndex, _reconConfig);
        }

        Joiner joiner = new Joiner(newColumnIndex, _newColumnName != null, newColumnIndex > columnModel.getKeyColumnIndex());

        Grid joined;
        if (Engine.Mode.RowBased.equals(_engineConfig.getMode())) {
            ChangeData<Cell> changeData = null;
            try {
                changeData = context.getChangeData(_changeDataId, new CellChangeDataSerializer(),
                        partialChangeData -> getChangeDataRowBased(projectState, baseColumnIndex, context, partialChangeData));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            joined = projectState.join(changeData, joiner, columnModel);
        } else {
            ChangeData<List<Cell>> changeData = null;
            try {
                changeData = context.getChangeData(_changeDataId, new CellListChangeDataSerializer(),
                        partialChangeData -> getChangeDataRecordBased(projectState, baseColumnIndex, context, partialChangeData));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            joined = projectState.join(changeData, joiner, columnModel);
        }
        return new ChangeResult(joined,
                GridPreservation.PRESERVES_ROWS, // TODO add record preservation metadata on Joiner
                null);
    }

    @Override
    public boolean isImmediate() {
        return true;
    }

    protected ChangeData<Cell> getChangeDataRowBased(Grid state, int columnIndex, ChangeContext changeContext,
            Optional<ChangeData<Cell>> partialChangeData) {
        ColumnModel columnModel = state.getColumnModel();
        Engine engine = new Engine(state, _engineConfig);

        RowInRecordChangeDataProducer<Cell> changeDataProducer = getChangeDataProducer(columnIndex, _columnName, columnModel,
                state.getOverlayModels(), changeContext);

        RowFilter filter = engine.combinedRowFilters();
        ChangeData<Cell> changeData = state.mapRows(filter, changeDataProducer, partialChangeData);
        return changeData;
    }

    protected ChangeData<List<Cell>> getChangeDataRecordBased(Grid state, int columnIndex, ChangeContext changeContext,
            Optional<ChangeData<List<Cell>>> partialChangeData) {
        ColumnModel columnModel = state.getColumnModel();
        Engine engine = new Engine(state, _engineConfig);

        RowInRecordChangeDataProducer<Cell> changeDataProducer = getChangeDataProducer(columnIndex, _columnName, columnModel,
                state.getOverlayModels(), changeContext);

        RecordFilter filter = engine.combinedRecordFilters();
        ChangeData<List<Cell>> changeData = state.mapRecords(filter, changeDataProducer, partialChangeData);
        return changeData;
    }

    public static class Joiner implements RowChangeDataJoiner<Cell>, RecordChangeDataJoiner<List<Cell>> {

        private static final long serialVersionUID = 8332780210267820528L;
        private final int _columnIndex;
        private final boolean _add;
        private final boolean _preservesRecords;

        public Joiner(int columnIndex, boolean add, boolean preservesRecords) {
            _columnIndex = columnIndex;
            _add = add;
            _preservesRecords = preservesRecords;
        }

        @Override
        public Row call(long rowId, Row row, Cell cell) {
            if (_add) {
                return row.insertCell(_columnIndex, cell);
            } else {
                if (cell != null) {
                    return row.withCell(_columnIndex, cell);
                } else {
                    return row;
                }
            }
        }

        @Override
        public boolean preservesRecordStructure() {
            return _preservesRecords;
        }

        @Override
        public List<Row> call(Record record, List<Cell> changeData) {
            List<Row> rows = record.getRows();
            List<Row> result = new ArrayList<>(rows.size());
            if (rows.size() != changeData.size()) {
                throw new IllegalArgumentException(
                        String.format("Change data and record do not have the same size at row %d", record.getStartRowId()));
            }
            for (int i = 0; i != rows.size(); i++) {
                result.add(call(record.getStartRowId() + i, rows.get(i), changeData.get(i)));
            }
            return result;
        }

    }

    public abstract RowInRecordChangeDataProducer<Cell> getChangeDataProducer(
            int columnIndex,
            String columnName,
            ColumnModel columnModel,
            Map<String, OverlayModel> overlayModels, ChangeContext changeContext);

    public static RowInRecordChangeDataProducer<Cell> evaluatingChangeDataProducer(
            int columnIndex,
            String baseColumnName,
            OnError onError,
            Evaluable eval,
            ColumnModel columnModel,
            Map<String, OverlayModel> overlayModels,
            long projectId) {
        return new RowInRecordChangeDataProducer<Cell>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Cell call(Record record, long rowId, Row row) {
                Cell cell = row.getCell(columnIndex);
                Cell newCell = null;

                Properties bindings = new Properties();
                ExpressionUtils.bind(bindings, columnModel, row, rowId, record, baseColumnName, cell, overlayModels);
                bindings.put("project_id", projectId);

                Object o = eval.evaluate(bindings);
                if (o != null) {
                    if (o instanceof Cell) {
                        newCell = (Cell) o;
                    } else if (o instanceof WrappedCell) {
                        newCell = ((WrappedCell) o).cell;
                    } else {
                        Serializable v = ExpressionUtils.wrapStorable(o);
                        if (ExpressionUtils.isError(v)) {
                            if (onError == OnError.SetToBlank) {
                                return null;
                            } else if (onError == OnError.KeepOriginal) {
                                v = cell != null ? cell.value : null;
                            }
                        }

                        if (v != null) {
                            newCell = new Cell(v, null);
                        }
                    }
                }
                return newCell;
            }

        };
    }

}
