package com.metaweb.gridworks.expr;

import java.util.Properties;

import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;

public class WrappedCell implements HasFields {
    final public Project project;
    final public String columnName;
    final public Cell cell;
    
    public WrappedCell(Project project, String columnName, Cell cell) {
        this.project = project;
        this.columnName = columnName;
        this.cell = cell;
    }
    
    public Object getField(String name, Properties bindings) {
        return cell.getField(name, bindings);
    }

    public boolean fieldAlsoHasFields(String name) {
        return cell.fieldAlsoHasFields(name);
    }
}
