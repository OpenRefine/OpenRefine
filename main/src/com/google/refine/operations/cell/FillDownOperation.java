package com.google.refine.operations.cell;

import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.CellChange;
import com.google.refine.operations.EngineDependentMassCellOperation;
import com.google.refine.operations.OperationRegistry;

public class FillDownOperation extends EngineDependentMassCellOperation {
    
    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        
        return new FillDownOperation(
            engineConfig,
            obj.getString("columnName")
        );
    }
    
    public FillDownOperation(
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
        return "Fill down cells in column " + _columnName;
    }

    protected String createDescription(Column column,
            List<CellChange> cellChanges) {
        
        return "Fill down " + cellChanges.size() + 
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
                    previousCell = row.getCell(cellIndex);
                } else if (previousCell != null) {
                    CellChange cellChange = new CellChange(rowIndex, cellIndex, row.getCell(cellIndex), previousCell);
                    cellChanges.add(cellChange);
                }
                return false;
            }
        }.init(column.getCellIndex(), cellChanges);
    }
}
