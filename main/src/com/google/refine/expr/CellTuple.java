package com.google.refine.expr;

import java.util.Properties;

import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class CellTuple implements HasFields {
    final public Project project;
    final public Row row;
    
    public CellTuple(Project project, Row row) {
        this.project = project;
        this.row = row;
    }
    
    public Object getField(String name, Properties bindings) {
        Column column = project.columnModel.getColumnByName(name);
        if (column != null) {
            int cellIndex = column.getCellIndex();
            Cell cell = row.getCell(cellIndex);
            
            if (cell != null) {
                return new WrappedCell(project, name, cell);
            }
        }
        return null;
    }

    public boolean fieldAlsoHasFields(String name) {
        return true;
    }
}