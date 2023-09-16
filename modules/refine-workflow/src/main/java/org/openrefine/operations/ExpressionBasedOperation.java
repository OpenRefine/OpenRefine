
package org.openrefine.operations;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.expr.WrappedCell;
import org.openrefine.history.History;
import org.openrefine.history.HistoryEntry;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Project;
import org.openrefine.model.Record;
import org.openrefine.model.RecordFilter;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.changes.CellChangeDataSerializer;
import org.openrefine.model.changes.CellListChangeDataSerializer;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.RowInRecordChangeDataProducer;
import org.openrefine.model.changes.RowMapChange;
import org.openrefine.operations.cell.TextTransformOperation;
import org.openrefine.operations.column.ColumnAdditionOperation;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.process.LongRunningProcess;
import org.openrefine.process.Process;
import org.openrefine.process.ProcessManager;
import org.openrefine.process.QuickHistoryEntryProcess;

/**
 * Base class for an operation which evaluates an expression on rows or records, and then uses it to derive the new
 * grid. This implements the common functionality of {@link ColumnAdditionOperation} and {@link TextTransformOperation}.
 * 
 * Depending on whether the expresion is local or not, the operation will return an immediate change or create a
 * long-running process which evaluates the expression and stores the results in a {@link ChangeData} object, so that
 * expensive evaluations (or expressions with side effects) are not evaluated multiple times.
 * 
 * @author Antonin Delpeuch
 *
 */
public abstract class ExpressionBasedOperation extends EngineDependentOperation {

    protected final String _expression;
    protected final String _baseColumnName;
    protected final OnError _onError;

    protected ExpressionBasedOperation(EngineConfig engineConfig, String expression, String baseColumnName, OnError onError) {
        super(engineConfig);
        _expression = expression;
        _baseColumnName = baseColumnName;
        _onError = onError;
    }

    /**
     * Returns the mapper to apply to all matching rows.
     * 
     * @param state
     * @return
     * @throws DoesNotApplyException
     */
    protected abstract RowInRecordMapper getPositiveRowMapper(Grid state, ChangeContext context, Evaluable eval)
            throws DoesNotApplyException;

    protected RowInRecordMapper getNegativeRowMapper(Grid state, ChangeContext context, Evaluable eval) throws DoesNotApplyException {
        return RowInRecordMapper.IDENTITY;
    }

    protected ColumnModel getNewColumnModel(Grid state, ChangeContext context, Evaluable eval) throws DoesNotApplyException {
        return state.getColumnModel();
    }

    protected int columnIndex(ColumnModel model, String columnName, Evaluable eval) throws DoesNotApplyException {
        return RowMapChange.columnIndex(model, columnName);
    }

    protected Grid postTransform(Grid state, ChangeContext context, Evaluable eval) {
        return state;
    }

    @Override
    public Change createChange() throws NotImmediateOperationException {
        Evaluable eval;
        try {
            eval = MetaParser.parse(_expression);
        } catch (ParsingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        // if the evaluator is a pure function which only reads data from the current row/record
        if (eval.isLocal()) {
            return createChange(eval);
        } else {
            // otherwise a long-running process is required
            throw new NotImmediateOperationException(
                    "Operation is not immediate because evaluable is not local, a long-running process is required");
        }
    }

    public Change createChange(Evaluable eval) {
        return new RowMapChange(getEngineConfig()) {

            @Override
            public RowInRecordMapper getPositiveRowMapper(Grid state, ChangeContext context) throws DoesNotApplyException {
                return ExpressionBasedOperation.this.getPositiveRowMapper(state, context, eval);
            }

            @Override
            public RowInRecordMapper getNegativeRowMapper(Grid state, ChangeContext context) throws DoesNotApplyException {
                return ExpressionBasedOperation.this.getNegativeRowMapper(state, context, eval);
            }

            @Override
            public ColumnModel getNewColumnModel(Grid state, ChangeContext context) throws DoesNotApplyException {
                return ExpressionBasedOperation.this.getNewColumnModel(state, context, eval);
            }

            @Override
            public Grid postTransform(Grid state, ChangeContext context) {
                return ExpressionBasedOperation.this.postTransform(state, context, eval);
            }

            @Override
            public boolean isImmediate() {
                return true;
            }

        };
    }

    @Override
    public Process createProcess(Project project) throws Exception {
        Evaluable eval;
        try {
            eval = MetaParser.parse(_expression);
        } catch (ParsingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        // if the evaluator is a pure function which only reads
        if (eval.isLocal()) {
            Change change = createChange(eval);
            return new QuickHistoryEntryProcess(project.getHistory(), getDescription(), this, change);
        } else {
            // otherwise, evaluating the expression might take a while and the results need to be stored
            return new ExpressionEvaluationProcess(
                    project.getHistory(),
                    project.getProcessManager(),
                    createEngine(project.getCurrentGrid()),
                    eval,
                    getDescription(),
                    project.getId());
        }
    }

    protected abstract Change getChangeForNonLocalExpression(String changeDataId, Evaluable evaluable, int columnIndex, Mode engineMode);

    /**
     * Long-running process to evaluate the expression and store the results as a ChangeData object.
     * 
     * @author Antonin Delpeuch
     *
     */
    protected class ExpressionEvaluationProcess extends LongRunningProcess implements Runnable {

        final protected History _history;
        final protected ProcessManager _manager;
        final protected Evaluable _eval;
        final protected Engine _engine;
        final protected long _historyEntryID;
        final protected long _projectId;

        public ExpressionEvaluationProcess(
                History history,
                ProcessManager manager,
                Engine engine,
                Evaluable eval,
                String description,
                long projectId) {
            super(description);
            _history = history;
            _manager = manager;
            _eval = eval;
            _engine = engine;
            _historyEntryID = HistoryEntry.allocateID();
            _projectId = projectId;
        }

        @Override
        public void run() {
            Grid state = _history.getCurrentGrid();
            ColumnModel columnModel = state.getColumnModel();

            int columnIndex = columnModel.getColumnIndexByName(_baseColumnName);
            RowInRecordChangeDataProducer<Cell> changeDataProducer = changeDataProducer(columnIndex, _baseColumnName, _onError, _eval,
                    columnModel, state.getOverlayModels(), _projectId);

            try {
                if (Mode.RowBased.equals(_engine.getMode())) {
                    RowFilter filter = _engine.combinedRowFilters();
                    ChangeData<Cell> changeData = state.mapRows(filter, changeDataProducer);
                    _history.getChangeDataStore().store(changeData, _historyEntryID, "eval", new CellChangeDataSerializer(),
                            Optional.of(_reporter));

                } else {
                    RecordFilter filter = _engine.combinedRecordFilters();
                    ChangeData<List<Cell>> changeData = state.mapRecords(filter, changeDataProducer);
                    _history.getChangeDataStore().store(changeData, _historyEntryID, "eval", new CellListChangeDataSerializer(),
                            Optional.of(_reporter));
                }

                if (!_canceled) {
                    Change change = getChangeForNonLocalExpression("eval", _eval, columnIndex, _engine.getMode());

                    _history.addEntry(
                            _historyEntryID,
                            _description,
                            ExpressionBasedOperation.this,
                            change);
                    _manager.onDoneProcess(this);
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (_canceled) {
                    _history.getChangeDataStore().discardAll(_historyEntryID);
                } else {
                    _manager.onFailedProcess(this, e);
                }
            }
        }

        @Override
        protected Runnable getRunnable() {
            return this;
        }
    }

    protected static RowInRecordChangeDataProducer<Cell> changeDataProducer(
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
