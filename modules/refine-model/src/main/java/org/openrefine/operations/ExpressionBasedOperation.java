
package org.openrefine.operations;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.Validate;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.expr.WrappedCell;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.*;
import org.openrefine.model.Record;
import org.openrefine.model.changes.CellChangeDataSerializer;
import org.openrefine.model.changes.CellListChangeDataSerializer;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.model.changes.RowInRecordChangeDataJoiner;
import org.openrefine.model.changes.RowInRecordChangeDataProducer;
import org.openrefine.operations.exceptions.MissingColumnException;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.util.ColumnDependencyException;

/**
 * Base class for an operation which evaluates an expression on rows or records, and then uses it to derive the new
 * grid.
 * 
 * Depending on whether the expression is local or not, the operation will compute it lazily (with a {@link RowMapper}
 * or {@link RecordMapper}) or will evaluate it eagerly and store the evaluation results in a {@link ChangeData} object,
 * so that expensive evaluations (or expressions with side effects) are not evaluated multiple times.
 * 
 * @author Antonin Delpeuch
 *
 */
public abstract class ExpressionBasedOperation extends RowMapOperation {

    protected final String _expression;
    protected final String _baseColumnName;
    protected final OnError _onError;
    protected final int _repeatCount;
    protected String _changeDataId = "eval";
    protected boolean _forceEagerEvaluation = false;

    // initialized lazily
    protected Evaluable _eval = null;

    protected ExpressionBasedOperation(EngineConfig engineConfig, String expression, String baseColumnName, OnError onError,
            int repeatCount) {
        super(engineConfig);
        _expression = expression;
        _baseColumnName = baseColumnName;
        _onError = onError;
        _repeatCount = repeatCount;
    }

    /**
     * Returns the joiner used to insert the result of the evaluation of the expression into a row or record. Depending
     * on whether the expression is local or not (see {@link Evaluable#isLocal()}), this will either be used to build a
     * row / record mapper to apply the change lazily on the grid, or the results of the evaluation will be stored in a
     * {@link ChangeData} object which will then be joined by this joiner.
     * 
     * @param grid
     *            the initial state of the grid
     */
    protected abstract RowInRecordChangeDataJoiner changeDataJoiner(Grid grid, ChangeContext context)
            throws OperationException;

    /**
     * Returns the new column model after the operation has run.
     */
    protected ColumnModel getNewColumnModel(Grid state, ChangeContext context, Evaluable eval) throws OperationException {
        return getNewColumnModel(state, context);
    }

    /**
     * Returns the new overlay models after this change is applied.
     */
    protected Map<String, OverlayModel> getNewOverlayModels(Grid state, ChangeContext context, Evaluable evaluable)
            throws OperationException {
        return getNewOverlayModels(state, context);
    }

    protected Grid postTransform(Grid state, ChangeContext context, Evaluable eval) throws OperationException {
        return postTransform(state, context);
    }

    @Override
    public ChangeResult apply(Grid projectState, ChangeContext context) throws OperationException {
        Evaluable eval = getEvaluable();
        if (eval.isLocal() && !_forceEagerEvaluation) {
            return super.apply(projectState, context);
        } else {
            RowInRecordChangeDataJoiner joiner = changeDataJoiner(projectState, context);
            ColumnModel newColumnModel = getNewColumnModel(projectState, context, eval);
            Engine engine = new Engine(projectState, _engineConfig, context.getProjectId());
            RowInRecordChangeDataProducer<Cell> producer = getChangeDataProducer(projectState, context);
            Grid joined;
            if (Engine.Mode.RowBased.equals(_engineConfig.getMode())) {
                ChangeData<Cell> changeData = null;
                try {
                    changeData = context.getChangeData(_changeDataId, new CellChangeDataSerializer(),
                            (grid, partialChangeData) -> {
                                Engine localEngine = new Engine(grid, _engineConfig, context.getProjectId());
                                RowFilter filter = localEngine.combinedRowFilters();
                                return grid.mapRows(filter, producer, partialChangeData);
                            },
                            producer.getColumnDependencies(), // TODO add dependencies from facets!
                            Engine.Mode.RowBased);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                joined = projectState.join(changeData, joiner, newColumnModel);
            } else {
                ChangeData<List<Cell>> changeData = null;
                try {
                    changeData = context.getChangeData(_changeDataId, new CellListChangeDataSerializer(),
                            (grid, partialChangeData) -> {
                                Engine localEngine = new Engine(grid, _engineConfig, context.getProjectId());
                                RecordFilter filter = localEngine.combinedRecordFilters();
                                return grid.mapRecords(filter, producer, partialChangeData);
                            },
                            producer.getColumnDependencies(),
                            Engine.Mode.RecordBased);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                joined = projectState.join(changeData, joiner, newColumnModel);
            }
            Map<String, OverlayModel> newOverlayModels = getNewOverlayModels(projectState, context, eval);
            return new ChangeResult(
                    postTransform(joined.withOverlayModels(newOverlayModels), context),
                    joiner.preservesRecordStructure() ? GridPreservation.PRESERVES_RECORDS : GridPreservation.PRESERVES_ROWS);
        }
    }

    protected Evaluable getEvaluable() throws OperationException {
        if (_eval == null) {
            try {
                _eval = MetaParser.parse(_expression);
            } catch (ParsingException e) {
                throw new OperationException("parsing", e.getMessage(), e);
            }
        }
        return _eval;
    }

    @Override
    protected GridMap getGridMap(Grid state, ChangeContext context) throws OperationException {
        Validate.notNull(_eval);
        RowInRecordChangeDataJoiner joiner = changeDataJoiner(state, context);
        PositiveRowMapper positiveMapper = new PositiveRowMapper(getChangeDataProducer(state, context), joiner, state.getColumnModel());
        NegativeRowMapper negativeMapper = new NegativeRowMapper(joiner);
        return new GridMap(
                getNewColumnModel(state, context, _eval),
                positiveMapper,
                negativeMapper,
                getNewOverlayModels(state, context, _eval));
    }

    protected RowInRecordChangeDataProducer<Cell> getChangeDataProducer(Grid state, ChangeContext context)
            throws OperationException {
        ColumnModel columnModel = state.getColumnModel();
        try {
            return evaluatingChangeDataProducer(_baseColumnName, _onError,
                    _repeatCount, _eval,
                    columnModel, state.getOverlayModels(), context.getProjectId());
        } catch (ColumnDependencyException e) {
            throw new MissingColumnException(e.getId().getColumnName());
        }
    }

    /**
     * Row mapper which calls a change data producer (to evaluate the expression) and directly calls the corresponding
     * change data joiner (to insert the results in the row). This should only be used with expressions which are local
     * and therefore cheap to evaluate.
     */
    protected static class PositiveRowMapper extends RowInRecordMapper {

        private static final long serialVersionUID = 696902213789742840L;
        RowInRecordChangeDataProducer<Cell> producer;
        RowInRecordChangeDataJoiner joiner;
        ColumnModel columnModel; // TODO: delete once RowMapper is migrated to a similar signature

        public PositiveRowMapper(RowInRecordChangeDataProducer<Cell> producer, RowInRecordChangeDataJoiner joiner,
                ColumnModel columnModel) {
            this.producer = producer;
            this.joiner = joiner;
            this.columnModel = columnModel;
        }

        @Override
        public Row call(Record record, long rowId, Row row) {
            IndexedData<Cell> indexedData = null;
            Cell producedCell = producer.call(record, rowId, row, columnModel);
            if (producedCell != null && producedCell.isPending()) {
                indexedData = new IndexedData<>(rowId);
            } else {
                indexedData = new IndexedData<>(rowId, producedCell);
            }
            return joiner.call(row, indexedData);
        }

        @Override
        public boolean preservesRecordStructure() {
            return joiner.preservesRecordStructure();
        }
    }

    protected static class NegativeRowMapper extends RowInRecordMapper {

        private static final long serialVersionUID = -2790780327244338473L;
        RowInRecordChangeDataJoiner joiner;

        public NegativeRowMapper(RowInRecordChangeDataJoiner joiner) {
            this.joiner = joiner;
        }

        @Override
        public Row call(Record record, long rowId, Row row) {
            return joiner.call(row, new IndexedData<>(rowId, null));
        }

        @Override
        public boolean preservesRecordStructure() {
            return joiner.preservesRecordStructure();
        }
    }

    public static RowInRecordChangeDataProducer<Cell> evaluatingChangeDataProducer(
            String baseColumnName,
            OnError onError,
            int repeatCount,
            Evaluable eval,
            ColumnModel columnModel,
            Map<String, OverlayModel> overlayModels,
            long projectId) {
        Set<String> columnNames = eval.getColumnDependencies(baseColumnName);
        List<ColumnId> dependencies = columnNames == null ? null
                : columnNames.stream()
                        .map(name -> columnModel.getColumnByName(name).getColumnId())
                        .collect(Collectors.toList());

        return new RowInRecordChangeDataProducer<Cell>() {

            private static final long serialVersionUID = 1L;

            @Override
            public List<ColumnId> getColumnDependencies() {
                return dependencies;
            }

            @Override
            public Cell call(Record translatedRecord, long rowId, Row translatedRow, ColumnModel columnModel) {
                int columnIndex = columnModel.getColumnIndexByName(baseColumnName);
                Cell cell = columnIndex == -1 ? null : translatedRow.getCell(columnIndex);
                Cell newCell = null;

                Properties bindings = new Properties();
                ExpressionUtils.bind(bindings, columnModel, translatedRow, rowId, translatedRecord,
                        baseColumnName, cell, overlayModels, projectId);
                // this should only happen when we are actually called by a row mapper and
                // not within the context of change data production
                if (ExpressionUtils.dependsOnPendingValues(eval, baseColumnName, columnModel, translatedRow, translatedRecord)) {
                    return Cell.PENDING_NULL;
                }

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
                                return Cell.NULL;
                            } else if (onError == OnError.KeepOriginal) {
                                v = cell != null ? cell.value : null;
                            }
                        }

                        if (v != null) {
                            newCell = new Cell(v, null);
                        }

                        for (int i = 0; i < repeatCount; i++) {
                            ExpressionUtils.bind(bindings, null, translatedRow, rowId, translatedRecord, baseColumnName, newCell,
                                    overlayModels, projectId);

                            v = ExpressionUtils.wrapStorable(eval.evaluate(bindings));
                            if (ExpressionUtils.isError(v)) {
                                break;
                            } else if (ExpressionUtils.sameValue(newCell.value, v)) {
                                break;
                            }

                            newCell = new Cell(v, newCell.recon);
                        }
                    }
                }
                if (newCell == null) {
                    newCell = Cell.NULL;
                }
                return newCell;
            }

        };
    }

}
