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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.Evaluable;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.ModelException;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.model.changes.RowInRecordChangeDataJoiner;
import org.openrefine.operations.ExpressionBasedOperation;
import org.openrefine.operations.OnError;
import org.openrefine.operations.exceptions.DuplicateColumnException;
import org.openrefine.operations.exceptions.OperationException;

/**
 * Adds a new column by evaluating an expression, based on a given column.
 * 
 * @author Antonin Delpeuch
 *
 */
public class ColumnAdditionOperation extends ExpressionBasedOperation {

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
        super(engineConfig, expression, baseColumnName, onError, 0);

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

    @Override
    protected ColumnModel getNewColumnModel(Grid state, ChangeContext context, Evaluable eval) throws OperationException {
        ColumnModel columnModel = state.getColumnModel();
        try {
            return columnModel.insertColumn(_columnInsertIndex, new ColumnMetadata(_newColumnName));
        } catch (ModelException e) {
            throw new DuplicateColumnException(_newColumnName);
        }
    }

    @Override
    protected RowInRecordChangeDataJoiner changeDataJoiner(Grid grid, ChangeContext context) throws OperationException {
        return new Joiner(_columnInsertIndex, grid.getColumnModel().getKeyColumnIndex());
    }

    public static class Joiner extends RowInRecordChangeDataJoiner {

        private static final long serialVersionUID = 5279645865937629998L;
        final int _columnIndex;
        final boolean _preservesRecords;

        public Joiner(int columnIndex, int keyColumnIndex) {
            _columnIndex = columnIndex;
            // TODO: if the key column index is not 0, it probably
            // needs shifting in the new grid if we are inserting the new column before it
            _preservesRecords = columnIndex > keyColumnIndex;
        }

        @Override
        public Row call(Row row, IndexedData<Cell> indexedData) {
            Cell cell = indexedData.getData();
            if (indexedData.isPending()) {
                cell = new Cell(null, null, true);
            }
            return row.insertCell(_columnIndex, cell);
        }

        @Override
        public boolean preservesRecordStructure() {
            return _preservesRecords;
        }

    }

    protected static RowInRecordMapper negativeMapper(int columnInsertIndex, int keyColumnIndex) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = -4885450470285627722L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                return row.insertCell(columnInsertIndex, null);
            }

            @Override
            public boolean preservesRecordStructure() {
                // TODO: if the key column index is not 0, it probably
                // needs shifting in the new grid if we are inserting the new column before it
                return columnInsertIndex > keyColumnIndex;
            }
        };
    }

}
