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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.Engine.Mode;
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
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ColumnChangeByChangeData;
import org.openrefine.model.changes.RowInRecordChangeDataProducer;
import org.openrefine.model.changes.RowMapChange;
import org.openrefine.operations.ExpressionBasedOperation;
import org.openrefine.operations.OnError;
import org.openrefine.overlay.OverlayModel;

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
        super(engineConfig, expression, baseColumnName, onError);

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
    protected ColumnModel getNewColumnModel(Grid state, ChangeContext context, Evaluable eval) throws DoesNotApplyException {
        ColumnModel columnModel = state.getColumnModel();
        try {
            return columnModel.insertColumn(_columnInsertIndex, new ColumnMetadata(_newColumnName));
        } catch (ModelException e) {
            throw new DoesNotApplyException("Another column already named " + _newColumnName);
        }
    }

    @Override
    protected RowInRecordMapper getPositiveRowMapper(Grid state, ChangeContext context, Evaluable eval)
            throws DoesNotApplyException {
        ColumnModel columnModel = state.getColumnModel();
        int columnIndex = RowMapChange.columnIndex(columnModel, _baseColumnName);
        return mapper(columnIndex, _baseColumnName, _columnInsertIndex, _onError, eval, columnModel, state.getOverlayModels());
    }

    @Override
    protected RowInRecordMapper getNegativeRowMapper(Grid state, ChangeContext context, Evaluable eval)
            throws DoesNotApplyException {
        return negativeMapper(_columnInsertIndex, state.getColumnModel().getKeyColumnIndex());
    }

    protected static RowInRecordMapper mapper(int columnIndex, String baseColumnName, int columnInsertIndex, OnError onError,
            Evaluable eval, ColumnModel columnModel, Map<String, OverlayModel> overlayModels) {
        RowInRecordChangeDataProducer<Cell> changeDataProducer = changeDataProducer(columnIndex, baseColumnName, onError, eval, columnModel,
                overlayModels,
                0L);
        return new RowInRecordMapper() {

            private static final long serialVersionUID = 897585827026825498L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                return row.insertCell(columnInsertIndex, changeDataProducer.call(record, rowId, row));
            }

            @Override
            public boolean preservesRecordStructure() {
                // TODO: if the key column index is not 0, it probably
                // needs shifting in the new grid if we are inserting the new column before it
                return columnInsertIndex > columnModel.getKeyColumnIndex();
            }

        };
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

    @Override
    protected Change getChangeForNonLocalExpression(String changeDataId, Evaluable evaluable, int columnIndex,
            Mode engineMode) {
        return new ColumnChangeByChangeData(
                "eval",
                columnIndex + 1,
                _newColumnName,
                engineMode,
                null);
    }
}
