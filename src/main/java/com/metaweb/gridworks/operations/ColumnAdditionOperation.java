package com.metaweb.gridworks.operations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.expr.MetaParser;
import com.metaweb.gridworks.expr.WrappedCell;
import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.changes.CellAtRow;
import com.metaweb.gridworks.model.changes.ColumnAdditionChange;

public class ColumnAdditionOperation extends EngineDependentOperation {
    final protected String     _baseColumnName;
    final protected String     _expression;
    final protected OnError    _onError;
    
    final protected String     _newColumnName;
    final protected int        _columnInsertIndex;

    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        
        return new ColumnAdditionOperation(
            engineConfig,
            obj.getString("baseColumnName"),
            obj.getString("expression"),
            TextTransformOperation.stringToOnError(obj.getString("onError")),
            obj.getString("newColumnName"),
            obj.getInt("columnInsertIndex")
        );
    }
    
    public ColumnAdditionOperation(
        JSONObject     engineConfig,
        String         baseColumnName,
        String         expression,
        OnError        onError,
        String         newColumnName, 
        int            columnInsertIndex 
    ) {
        super(engineConfig);
        
        _baseColumnName = baseColumnName;
        _expression = expression;
        _onError = onError;
        
        _newColumnName = newColumnName;
        _columnInsertIndex = columnInsertIndex;
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("engineConfig"); writer.value(getEngineConfig());
        writer.key("newColumnName"); writer.value(_newColumnName);
        writer.key("columnInsertIndex"); writer.value(_columnInsertIndex);
        writer.key("baseColumnName"); writer.value(_baseColumnName);
        writer.key("expression"); writer.value(_expression);
        writer.key("onError"); writer.value(TextTransformOperation.onErrorToString(_onError));
        writer.endObject();
    }

    protected String getBriefDescription(Project project) {
        return "Create column " + _newColumnName + 
            " at index " + _columnInsertIndex + 
            " based on column " + _baseColumnName + 
            " using expression " + _expression;
    }

    protected String createDescription(Column column, List<CellAtRow> cellsAtRows) {
        return "Create new column " + _newColumnName + 
            " based on column " + column.getName() + 
            " by filling " + cellsAtRows.size() +
            " rows with " + _expression;
    }
    
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        Engine engine = createEngine(project);
        
        Column column = project.columnModel.getColumnByName(_baseColumnName);
        if (column == null) {
            throw new Exception("No column named " + _baseColumnName);
        }
        
        List<CellAtRow> cellsAtRows = new ArrayList<CellAtRow>(project.rows.size());
        
        FilteredRows filteredRows = engine.getAllFilteredRows(false);
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
            int              cellIndex;
            Properties       bindings;
            List<CellAtRow>  cellsAtRows;
            Evaluable        eval;
            
            public RowVisitor init(int cellIndex, Properties bindings, List<CellAtRow> cellsAtRows, Evaluable eval) {
                this.cellIndex = cellIndex;
                this.bindings = bindings;
                this.cellsAtRows = cellsAtRows;
                this.eval = eval;
                return this;
            }
            
            public boolean visit(Project project, int rowIndex, Row row, boolean includeContextual, boolean includeDependent) {
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
