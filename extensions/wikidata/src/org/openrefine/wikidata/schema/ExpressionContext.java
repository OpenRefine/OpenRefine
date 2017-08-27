package org.openrefine.wikidata.schema;

import com.google.refine.model.Cell;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.Row;


public class ExpressionContext {
    private String baseIRI;
    private Row row;
    private ColumnModel columnModel;
    
    public ExpressionContext(String baseIRI, Row row, ColumnModel columnModel) {
        this.baseIRI = baseIRI;
        this.row = row;
        this.columnModel = columnModel;
    }
    
    public String getBaseIRI() {
        return baseIRI;
    }
    
    public Cell getCellByName(String name) {
        int idx = columnModel.getColumnByName(name).getCellIndex();
        return row.getCell(idx);
    }
}
