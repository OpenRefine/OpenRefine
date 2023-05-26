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

package org.openrefine.operations.cell;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Row;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.model.changes.RowInRecordChangeDataJoiner;
import org.openrefine.operations.ExpressionBasedOperation;
import org.openrefine.operations.OnError;
import org.openrefine.operations.Operation;
import org.openrefine.operations.Operation.DoesNotApplyException;

public class TextTransformOperation extends ExpressionBasedOperation {

    @JsonProperty("repeat")
    final protected boolean _repeat;

    static public OnError stringToOnError(String s) {
        if ("set-to-blank".equalsIgnoreCase(s)) {
            return OnError.SetToBlank;
        } else if ("store-error".equalsIgnoreCase(s)) {
            return OnError.StoreError;
        } else {
            return OnError.KeepOriginal;
        }
    }

    static public String onErrorToString(OnError onError) {
        if (onError == OnError.SetToBlank) {
            return "set-to-blank";
        } else if (onError == OnError.StoreError) {
            return "store-error";
        } else {
            return "keep-original";
        }
    }

    @JsonCreator
    public TextTransformOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("expression") String expression,
            @JsonProperty("onError") OnError onError,
            @JsonProperty("repeat") boolean repeat,
            @JsonProperty("repeatCount") int repeatCount) {
        super(engineConfig, expression, columnName, onError, repeat ? repeatCount : 0);
        _repeat = repeat;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
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

    @JsonProperty("repeatCount")
    public int getRepeatCount() {
        return _repeatCount;
    }

    @Override
    public String getDescription() {
        return "Text transform on cells in column " + _baseColumnName + " using expression " + _expression;
    }

    @Override
    protected RowInRecordChangeDataJoiner changeDataJoiner(Grid grid, ChangeContext context) throws Operation.DoesNotApplyException {
        ColumnModel columnModel = grid.getColumnModel();
        int baseColumnIndex = columnIndex(columnModel, _baseColumnName);
        return new Joiner(baseColumnIndex, columnModel.getKeyColumnIndex());
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
                Cell currentCell = row.getCell(_columnIndex);
                cell = new Cell(
                        currentCell == null ? null : currentCell.value,
                        currentCell == null ? null : currentCell.recon,
                        true);
            }
            if (cell != null) {
                return row.withCell(_columnIndex, cell);
            } else {
                return row;
            }
        }

        @Override
        public boolean preservesRecordStructure() {
            return _preservesRecords;
        }
    }

}
