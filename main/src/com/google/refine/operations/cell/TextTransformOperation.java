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

package com.google.refine.operations.cell;

import java.io.Serializable;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.WrappedCell;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.CellChange;
import com.google.refine.operations.EngineDependentMassCellOperation;
import com.google.refine.operations.OnError;
import com.google.refine.operations.OperationRegistry;

public class TextTransformOperation extends EngineDependentMassCellOperation {
    final protected String  _expression;
    final protected OnError _onError;
    final protected boolean _repeat;
    final protected int     _repeatCount;
    
    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        
        return new TextTransformOperation(
            engineConfig,
            obj.getString("columnName"),
            obj.getString("expression"),
            stringToOnError(obj.getString("onError")),
            obj.getBoolean("repeat"),
            obj.getInt("repeatCount")
        );
    }
    
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
    
    public TextTransformOperation(
            JSONObject engineConfig, 
            String columnName, 
            String expression, 
            OnError onError,
            boolean repeat,
            int repeatCount
        ) {
        super(engineConfig, columnName, true);
        _expression = expression;
        _onError = onError;
        _repeat = repeat;
        _repeatCount = repeatCount;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("engineConfig"); writer.value(getEngineConfig());
        writer.key("columnName"); writer.value(_columnName);
        writer.key("expression"); writer.value(_expression);
        writer.key("onError"); writer.value(onErrorToString(_onError));
        writer.key("repeat"); writer.value(_repeat);
        writer.key("repeatCount"); writer.value(_repeatCount);
        writer.endObject();
    }

    @Override
    protected String getBriefDescription(Project project) {
        return "Text transform on cells in column " + _columnName + " using expression " + _expression;
    }

    @Override
    protected String createDescription(Column column,
            List<CellChange> cellChanges) {
        
        return "Text transform on " + cellChanges.size() + 
            " cells in column " + column.getName() + ": " + _expression;
    }

    @Override
    protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges, long historyEntryID) throws Exception {
        Column column = project.columnModel.getColumnByName(_columnName);
        
        Evaluable eval = MetaParser.parse(_expression);
        Properties bindings = ExpressionUtils.createBindings(project);
        
        return new RowVisitor() {
            int                 cellIndex;
            Properties             bindings;
            List<CellChange>     cellChanges;
            Evaluable             eval;
            
            public RowVisitor init(int cellIndex, Properties bindings, List<CellChange> cellChanges, Evaluable eval) {
                this.cellIndex = cellIndex;
                this.bindings = bindings;
                this.cellChanges = cellChanges;
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

                Object oldValue = cell != null ? cell.value : null;

                ExpressionUtils.bind(bindings, row, rowIndex, _columnName, cell);

                Object o = eval.evaluate(bindings);
                if (o == null) {
                    if (oldValue != null) {
                        CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, null);
                        cellChanges.add(cellChange);
                    }
                } else {
                    if (o instanceof Cell) {
                        newCell = (Cell) o;
                    } else if (o instanceof WrappedCell) {
                        newCell = ((WrappedCell) o).cell;
                    } else {
                        Serializable newValue = ExpressionUtils.wrapStorable(o);
                        if (ExpressionUtils.isError(newValue)) {
                            if (_onError == OnError.KeepOriginal) {
                                return false;
                            } else if (_onError == OnError.SetToBlank) {
                                newValue = null;
                            }
                        }
                        
                        if (!ExpressionUtils.sameValue(oldValue, newValue)) {
                            newCell = new Cell(newValue, (cell != null) ? cell.recon : null);
                            
                            if (_repeat) {
                                for (int i = 0; i < _repeatCount; i++) {
                                    ExpressionUtils.bind(bindings, row, rowIndex, _columnName, newCell);
                                    
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
                    
                    if (newCell != null) {
                        CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, newCell);
                        cellChanges.add(cellChange);
                    }
                }
                
                return false;
            }
        }.init(column.getCellIndex(), bindings, cellChanges, eval);
    }
}
