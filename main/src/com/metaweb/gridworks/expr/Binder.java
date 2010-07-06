package com.metaweb.gridworks.expr;

import java.util.Properties;

import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public interface Binder {
    public void initializeBindings(Properties bindings, Project project);
        
    public void bind(Properties bindings, Row row, int rowIndex, String columnName, Cell cell);
}
