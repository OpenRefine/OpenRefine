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

import java.io.Serializable;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.expr.WrappedCell;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ColumnChangeByChangeData;
import org.openrefine.model.changes.RowMapChange;
import org.openrefine.operations.ExpressionBasedOperation;
import org.openrefine.operations.OnError;

public class TextTransformOperation extends ExpressionBasedOperation {

    @JsonProperty("repeat")
    final protected boolean _repeat;
    @JsonProperty("repeatCount")
    final protected int _repeatCount;

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
        super(engineConfig, expression, columnName, onError);
        _repeat = repeat;
        _repeatCount = repeatCount;
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

    @Override
    public String getDescription() {
        return "Text transform on cells in column " + _baseColumnName + " using expression " + _expression;
    }

    @Override
    protected RowInRecordMapper getPositiveRowMapper(GridState state, ChangeContext context, Evaluable eval) throws DoesNotApplyException {
        int columnIndex = RowMapChange.columnIndex(state.getColumnModel(), _baseColumnName);
        return rowMapper(columnIndex, _baseColumnName, state.getColumnModel(), eval, _onError, _repeat ? _repeatCount : 0);
    }

    @Override
    protected Change getChangeForNonLocalExpression(String changeDataId, Evaluable evaluable, int columnIndex,
            Mode engineMode) {
        return new ColumnChangeByChangeData(
                "eval",
                columnIndex,
                null,
                engineMode,
                null,
                null);
    }

    protected static RowInRecordMapper rowMapper(int columnIndex, String columnName, ColumnModel columnModel, Evaluable eval,
            OnError onError, int repeatCount) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = 2272064171042189466L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                Cell cell = row.getCell(columnIndex);
                Cell newCell = null;

                Object oldValue = cell != null ? cell.value : null;
                Properties bindings = new Properties();
                ExpressionUtils.bind(bindings, columnModel, row, rowId, record, columnName, cell);

                Object o = eval.evaluate(bindings);
                if (o == null) {
                    newCell = null;
                } else {
                    if (o instanceof Cell) {
                        newCell = (Cell) o;
                    } else if (o instanceof WrappedCell) {
                        newCell = ((WrappedCell) o).cell;
                    } else {
                        Serializable newValue = ExpressionUtils.wrapStorable(o);
                        if (ExpressionUtils.isError(newValue)) {
                            if (onError == OnError.KeepOriginal) {
                                return row;
                            } else if (onError == OnError.SetToBlank) {
                                newValue = null;
                            }
                        }

                        if (!ExpressionUtils.sameValue(oldValue, newValue)) {
                            newCell = new Cell(newValue, (cell != null) ? cell.recon : null);

                            for (int i = 0; i < repeatCount; i++) {
                                ExpressionUtils.bind(bindings, null, row, rowId, record, columnName, newCell);

                                newValue = ExpressionUtils.wrapStorable(eval.evaluate(bindings));
                                if (ExpressionUtils.isError(newValue)) {
                                    break;
                                } else if (ExpressionUtils.sameValue(newCell.value, newValue)) {
                                    break;
                                }

                                newCell = new Cell(newValue, newCell.recon);
                            }
                        }
                    }
                }

                return row.withCell(columnIndex, newCell);
            }

        };
    }

}
