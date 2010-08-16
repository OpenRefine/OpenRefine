package com.google.gridworks.operations.cell;

import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.gridworks.browsing.RowVisitor;
import com.google.gridworks.expr.ExpressionUtils;
import com.google.gridworks.model.AbstractOperation;
import com.google.gridworks.model.Cell;
import com.google.gridworks.model.Column;
import com.google.gridworks.model.Project;
import com.google.gridworks.model.Row;
import com.google.gridworks.model.changes.CellChange;
import com.google.gridworks.operations.EngineDependentMassCellOperation;
import com.google.gridworks.operations.OperationRegistry;

public class BlankDownOperation extends EngineDependentMassCellOperation {
    
    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        
        return new BlankDownOperation(
            engineConfig,
            obj.getString("columnName")
        );
    }
    
    public BlankDownOperation(
            JSONObject engineConfig, 
            String columnName
        ) {
        super(engineConfig, columnName, true);
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("engineConfig"); writer.value(getEngineConfig());
        writer.key("columnName"); writer.value(_columnName);
        writer.endObject();
    }

    protected String getBriefDescription(Project project) {
        return "Blank down cells in column " + _columnName;
    }

    protected String createDescription(Column column,
            List<CellChange> cellChanges) {
        
        return "Blank down " + cellChanges.size() + 
            " cells in column " + column.getName();
    }

    protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges, long historyEntryID) throws Exception {
        Column column = project.columnModel.getColumnByName(_columnName);
        
        return new RowVisitor() {
            int                 cellIndex;
            List<CellChange>    cellChanges;
            Cell                previousCell;
            
            public RowVisitor init(int cellIndex, List<CellChange> cellChanges) {
                this.cellIndex = cellIndex;
                this.cellChanges = cellChanges;
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
            
            public boolean visit(Project project, int rowIndex, Row row) {
                Object value = row.getCellValue(cellIndex);
                if (ExpressionUtils.isNonBlankData(value)) {
                    Cell cell = row.getCell(cellIndex);
                    if (previousCell != null && cell.value.equals(previousCell.value)) {
                        CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, null);
                        cellChanges.add(cellChange);
                    }
                    previousCell = cell;
                } else {
                    previousCell = null;
                }
                return false;
            }
        }.init(column.getCellIndex(), cellChanges);
    }
}
