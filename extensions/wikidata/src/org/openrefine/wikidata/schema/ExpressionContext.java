package org.openrefine.wikidata.schema;

import com.google.refine.model.Cell;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.Row;


public class ExpressionContext {
    private String baseIRI;
    private int rowId;
    private Row row;
    private ColumnModel columnModel;
    
    public ExpressionContext(String baseIRI, int rowId, Row row, ColumnModel columnModel) {
        this.baseIRI = baseIRI;
        this.rowId = rowId;
        this.row = row;
        this.columnModel = columnModel;
    }
    
    public String getBaseIRI() {
        return baseIRI;
    }
    
    public int getCellIndexByName(String name) {
        return columnModel.getColumnByName(name).getCellIndex();
    }
    
    public Cell getCellByName(String name) {
        int idx = getCellIndexByName(name);
        return row.getCell(idx);
    }
    
    public int getRowId() {
        return rowId;
    }
}
