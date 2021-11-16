/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.operations.column;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.ModelException;
import org.openrefine.model.Project;
import org.openrefine.model.Record;
import org.openrefine.model.RecordFilter;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.changes.CellAtRow;
import org.openrefine.model.changes.CellChangeDataSerializer;
import org.openrefine.model.changes.CellListChangeDataSerializer;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ColumnChangeByChangeData;
import org.openrefine.model.changes.RowInRecordChangeDataProducer;
import org.openrefine.model.changes.RowMapChange;
import org.openrefine.operations.EngineDependentOperation;
import org.openrefine.operations.OnError;
import org.openrefine.process.LongRunningProcess;
import org.openrefine.process.Process;
import org.openrefine.process.ProcessManager;
import org.openrefine.process.QuickHistoryEntryProcess;

public class ColumnAdditionOperation extends EngineDependentOperation {

    final protected String _baseColumnName;
    final protected String _expression;
    final protected OnError _onError;

    final protected String _newColumnName;
    final protected int _columnInsertIndex;

    @JsonCreator
    public ColumnAdditionOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("baseColumnName") String baseColumnName,
            @JsonProperty("expression") String expression,
            @JsonProperty("onError") OnError onError,
            @JsonProperty("newColumnName") String newColumnName,
            @JsonProperty("columnInsertIndex") int columnInsertIndex) {
        super(engineConfig);

        _baseColumnName = baseColumnName;
        _expression = expression;
        _onError = onError;

        _newColumnName = newColumnName;
        _columnInsertIndex = columnInsertIndex;
    }

    @JsonProperty("newColumnName")
    public String getNewColumnName() {
        return _newColumnName;
    }

    @JsonProperty("columnInsertIndex")
    public int getColumnInsertIndex() {
        return _columnInsertIndex;
    }

    @JsonProperty("baseColumnName")
    public String getBaseColumnName() {
        return _baseColumnName;
    }

    @JsonProperty("expression")
    public String getExpression() {
        return _expression;
    }

    @JsonProperty("onError")
    public OnError getOnError() {
        return _onError;
    }

    @Override
    public String getDescription() {
        return "Create column " + _newColumnName +
                " at index " + _columnInsertIndex +
                " based on column " + _baseColumnName +
                " using expression " + _expression;
    }

    protected String createDescription(ColumnMetadata column, List<CellAtRow> cellsAtRows) {
        return "Create new column " + _newColumnName +
                " based on column " + column.getName() +
                " by filling " + cellsAtRows.size() +
                " rows with " + _expression;
    }

    protected ColumnModel getNewColumnModel(GridState state, ChangeContext context) throws DoesNotApplyException {
        ColumnModel columnModel = state.getColumnModel();
        try {
            return columnModel.insertColumn(_columnInsertIndex, new ColumnMetadata(_newColumnName));
        } catch (ModelException e) {
            throw new DoesNotApplyException("Another column already named " + _newColumnName);
        }
    }

    @Override
    public Change createChange() throws NotImmediateOperationException, ParsingException {
        Evaluable eval;
        try {
            eval = MetaParser.parse(_expression);
        } catch (ParsingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        // if the evaluator is a pure function which only reads
        if (eval.isLocal()) {
            return createChange(eval);
        } else {
            throw new NotImmediateOperationException("Operation is not immediate because evaluable is not local");
        }
    }

    private Change createChange(Evaluable eval) {
        return new RowMapChange(getEngineConfig()) {

            @Override
            public RowInRecordMapper getPositiveRowMapper(GridState state, ChangeContext context) throws DoesNotApplyException {
                ColumnModel columnModel = state.getColumnModel();
                int columnIndex = columnIndex(columnModel, _baseColumnName);
                return mapper(columnIndex, _baseColumnName, _columnInsertIndex, _onError, eval, columnModel);
            }

            @Override
            public RowInRecordMapper getNegativeRowMapper(GridState state, ChangeContext context) throws DoesNotApplyException {
                return negativeMapper(_columnInsertIndex);
            }

            @Override
            public ColumnModel getNewColumnModel(GridState state, ChangeContext context) throws DoesNotApplyException {
                return ColumnAdditionOperation.this.getNewColumnModel(state, context);
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
            return new ColumnAdditionProcess(
                    project.getHistory(),
                    project.getProcessManager(),
                    createEngine(project.getCurrentGridState()),
                    eval,
                    getDescription(),
                    project.getId());
        }
    };

    protected static RowInRecordMapper mapper(int columnIndex, String baseColumnName, int columnInsertIndex, OnError onError,
            Evaluable eval, ColumnModel columnModel) {
        RowInRecordChangeDataProducer<Cell> changeDataProducer = changeDataProducer(columnIndex, baseColumnName, onError, eval, columnModel,
                0L);
        return new RowInRecordMapper() {

            private static final long serialVersionUID = 897585827026825498L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                return row.insertCell(columnInsertIndex, changeDataProducer.call(record, rowId, row));
            }

        };
    }

    protected static RowInRecordMapper negativeMapper(int columnInsertIndex) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = -4885450470285627722L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                return row.insertCell(columnInsertIndex, null);
            }

        };
    }

    /**
     * Long-running process
     * 
     * @author Antonin Delpeuch
     *
     */
    protected class ColumnAdditionProcess extends LongRunningProcess implements Runnable {

        final protected History _history;
        final protected ProcessManager _manager;
        final protected Evaluable _eval;
        final protected Engine _engine;
        final protected long _historyEntryID;
        final protected long _projectId;

        public ColumnAdditionProcess(
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
            GridState state = _history.getCurrentGridState();
            ColumnModel columnModel = state.getColumnModel();

            int columnIndex = columnModel.getColumnIndexByName(_baseColumnName);
            RowInRecordChangeDataProducer<Cell> changeDataProducer = changeDataProducer(columnIndex, _baseColumnName, _onError, _eval,
                    columnModel, _projectId);

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
                    Change change = new ColumnChangeByChangeData(
                            "eval",
                            columnIndex + 1,
                            _newColumnName,
                            _engine.getMode(),
                            null,
                            null);

                    HistoryEntry historyEntry = new HistoryEntry(
                            _historyEntryID,
                            _description,
                            ColumnAdditionOperation.this,
                            change);

                    _history.addEntry(historyEntry);
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
            long projectId) {
        return new RowInRecordChangeDataProducer<Cell>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Cell call(Record record, long rowId, Row row) {
                Cell cell = row.getCell(columnIndex);
                Cell newCell = null;

                Properties bindings = new Properties();
                ExpressionUtils.bind(bindings, columnModel, row, rowId, record, baseColumnName, cell);
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
