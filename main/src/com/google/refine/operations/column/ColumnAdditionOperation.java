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

package com.google.refine.operations.column;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.WrappedCell;
import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.CellAtRow;
import com.google.refine.model.changes.ColumnAdditionChange;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.operations.OnError;
import com.google.refine.operations.OperationDescription;

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
    protected String getBriefDescription(Project project) {
        return OperationDescription.column_addition_brief(_newColumnName, _columnInsertIndex, _baseColumnName, _expression);
    }

    protected String createDescription(Column column, List<CellAtRow> cellsAtRows) {
        return OperationDescription.column_addition_desc(_newColumnName, column.getName(), cellsAtRows.size(), _expression);
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        Engine engine = createEngine(project);

        Column column = project.columnModel.getColumnByName(_baseColumnName);
        if (column == null) {
            throw new Exception("No column named " + _baseColumnName);
        }
        if (project.columnModel.getColumnByName(_newColumnName) != null) {
            throw new Exception("Another column already named " + _newColumnName);
        }

        List<CellAtRow> cellsAtRows = new ArrayList<CellAtRow>(project.rows.size());

        FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(project, createRowVisitor(project, cellsAtRows));

        String description = createDescription(column, cellsAtRows);

        Change change = new ColumnAdditionChange(_newColumnName, _columnInsertIndex, cellsAtRows);

        return new HistoryEntry(
                historyEntryID, project, description, this, change);
    }

    protected RowVisitor createRowVisitor(Project project, List<CellAtRow> cellsAtRows) throws Exception {
        Column column = project.columnModel.getColumnByName(_baseColumnName);

        Evaluable eval = MetaParser.parse(_expression);
        Properties bindings = ExpressionUtils.createBindings(project);

        return new RowVisitor() {

            int cellIndex;
            Properties bindings;
            List<CellAtRow> cellsAtRows;
            Evaluable eval;

            public RowVisitor init(int cellIndex, Properties bindings, List<CellAtRow> cellsAtRows, Evaluable eval) {
                this.cellIndex = cellIndex;
                this.bindings = bindings;
                this.cellsAtRows = cellsAtRows;
                this.eval = eval;
                return this;
            }

            @Override
            public void start(Project project) {
                // nothing to do
            }

            @Override
            public void end(Project project) {
                // nothing to do
            }

            @Override
            public boolean visit(Project project, int rowIndex, Row row) {
                Cell cell = row.getCell(cellIndex);
                Cell newCell = null;

                ExpressionUtils.bind(bindings, row, rowIndex, _baseColumnName, cell);

                Object o = eval.evaluate(bindings);
                if (o != null) {
                    if (o instanceof Cell) {
                        newCell = (Cell) o;
                    } else if (o instanceof WrappedCell) {
                        newCell = ((WrappedCell) o).cell;
                    } else {
                        Serializable v = ExpressionUtils.wrapStorable(o);
                        if (ExpressionUtils.isError(v)) {
                            if (_onError == OnError.SetToBlank) {
                                return false;
                            } else if (_onError == OnError.KeepOriginal) {
                                v = cell != null ? cell.value : null;
                            }
                        }

                        if (v != null) {
                            newCell = new Cell(v, null);
                        }
                    }
                }

                if (newCell != null) {
                    cellsAtRows.add(new CellAtRow(rowIndex, newCell));
                }

                return false;
            }
        }.init(column.getCellIndex(), bindings, cellsAtRows, eval);
    }
}
