package com.metaweb.gridworks.operations;

import java.io.Serializable;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.expr.MetaParser;
import com.metaweb.gridworks.expr.WrappedCell;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.changes.CellChange;

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

    protected String getBriefDescription(Project project) {
        return "Text transform on cells in column " + _columnName + " using expression " + _expression;
    }

    protected String createDescription(Column column,
            List<CellChange> cellChanges) {
        
        return "Text transform on " + cellChanges.size() + 
            " cells in column " + column.getName() + ": " + _expression;
    }

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
            
            public boolean visit(Project project, int rowIndex, Row row, boolean includeContextual, boolean includeDependent) {
                Cell cell = row.getCell(cellIndex);
                Cell newCell = null;
                
                Object oldValue = cell != null ? cell.value : null;

                ExpressionUtils.bind(bindings, row, rowIndex, _columnName, cell);
                
                Object o = eval.evaluate(bindings);
                if (o != null) {
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
