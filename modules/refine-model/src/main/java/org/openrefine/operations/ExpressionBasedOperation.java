
package org.openrefine.operations;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.Validate;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.expr.WrappedCell;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Record;
import org.openrefine.model.RecordFilter;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.changes.CellChangeDataSerializer;
import org.openrefine.model.changes.CellListChangeDataSerializer;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.model.changes.RowInRecordChangeDataJoiner;
import org.openrefine.model.changes.RowInRecordChangeDataProducer;
import org.openrefine.operations.Operation.ChangeResult;
import org.openrefine.operations.Operation.DoesNotApplyException;
import org.openrefine.overlay.OverlayModel;

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
            throws Operation.DoesNotApplyException;

    /**
     * Returns the new column model after the operation has run.
     */
    protected ColumnModel getNewColumnModel(Grid state, ChangeContext context, Evaluable eval) throws Operation.DoesNotApplyException {
        return getNewColumnModel(state, context);
    }

    /**
     * Returns the new overlay models after this change is applied.
     */
    protected Map<String, OverlayModel> getNewOverlayModels(Grid state, ChangeContext context, Evaluable evaluable)
            throws Operation.DoesNotApplyException {
        return getNewOverlayModels(state, context);
    }

    protected Grid postTransform(Grid state, ChangeContext context, Evaluable eval) throws DoesNotApplyException {
        return postTransform(state, context);
    }

    @Override
    public Operation.ChangeResult apply(Grid projectState, ChangeContext context) throws ParsingException, Operation.DoesNotApplyException {
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
                            partialChangeData -> {
                                RowFilter filter = engine.combinedRowFilters();
                                return projectState.mapRows(filter, producer, partialChangeData);
                            });
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                joined = projectState.join(changeData, joiner, newColumnModel);
            } else {
                ChangeData<List<Cell>> changeData = null;
                try {
                    changeData = context.getChangeData(_changeDataId, new CellListChangeDataSerializer(),
                            partialChangeData -> {
                                RecordFilter filter = engine.combinedRecordFilters();
                                return projectState.mapRecords(filter, producer, partialChangeData);
                            });
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                joined = projectState.join(changeData, joiner, newColumnModel);
            }
            Map<String, OverlayModel> newOverlayModels = getNewOverlayModels(projectState, context, eval);
            return new Operation.ChangeResult(
                    postTransform(joined.withOverlayModels(newOverlayModels), context),
                    joiner.preservesRecordStructure() ? GridPreservation.PRESERVES_RECORDS : GridPreservation.PRESERVES_ROWS);
        }
    }

    protected Evaluable getEvaluable() throws ParsingException {
        if (_eval == null) {
            _eval = MetaParser.parse(_expression);
        }
        return _eval;
    }

    @Override
    protected GridMap getGridMap(Grid state, ChangeContext context) throws Operation.DoesNotApplyException {
        Validate.notNull(_eval);
        RowInRecordChangeDataJoiner joiner = changeDataJoiner(state, context);
        PositiveRowMapper positiveMapper = new PositiveRowMapper(getChangeDataProducer(state, context), joiner);
        NegativeRowMapper negativeMapper = new NegativeRowMapper(joiner);
        return new GridMap(
                getNewColumnModel(state, context, _eval),
                positiveMapper,
                negativeMapper,
                getNewOverlayModels(state, context, _eval));
    }

    protected RowInRecordChangeDataProducer<Cell> getChangeDataProducer(Grid state, ChangeContext context)
            throws Operation.DoesNotApplyException {
        ColumnModel columnModel = state.getColumnModel();
        int baseColumnIndex = columnModel.getColumnIndexByName(_baseColumnName);
        if (baseColumnIndex == -1) {
            throw new Operation.DoesNotApplyException(String.format("Column '{}' not found", _baseColumnName));
        }
        RowInRecordChangeDataProducer<Cell> producer = evaluatingChangeDataProducer(baseColumnIndex, _baseColumnName, _onError,
                _repeatCount, _eval,
                columnModel, state.getOverlayModels(), context.getProjectId());
        return producer;
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

        public PositiveRowMapper(RowInRecordChangeDataProducer<Cell> producer, RowInRecordChangeDataJoiner joiner) {
            this.producer = producer;
            this.joiner = joiner;
        }

        @Override
        public Row call(Record record, long rowId, Row row) {
            IndexedData<Cell> indexedData = null;
            Cell producedCell = producer.call(record, rowId, row);
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
            return joiner.call(row, null);
        }

        @Override
        public boolean preservesRecordStructure() {
            return joiner.preservesRecordStructure();
        }
    }

    public static RowInRecordChangeDataProducer<Cell> evaluatingChangeDataProducer(
            int columnIndex,
            String baseColumnName,
            OnError onError,
            int repeatCount,
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
                ExpressionUtils.bind(bindings, columnModel, row, rowId, record, baseColumnName, cell, overlayModels, projectId);
                // this should only happen when we are actually called by a row mapper and
                // not within the context of change data production
                if (ExpressionUtils.dependsOnPendingValues(eval, baseColumnName, columnModel, row, record)) {
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
                            ExpressionUtils.bind(bindings, null, row, rowId, record, baseColumnName, newCell, overlayModels, projectId);

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
                return newCell;
            }

        };
    }

}
