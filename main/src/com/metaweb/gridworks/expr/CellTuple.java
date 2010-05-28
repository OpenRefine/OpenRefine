package com.metaweb.gridworks.expr;

import java.util.Properties;

import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

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