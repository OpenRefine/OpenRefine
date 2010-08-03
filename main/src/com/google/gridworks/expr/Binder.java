package com.google.gridworks.expr;

import java.util.Properties;

import com.google.gridworks.model.Cell;
import com.google.gridworks.model.Project;
import com.google.gridworks.model.Row;

public interface Binder {
    public void initializeBindings(Properties bindings, Project project);
        
    public void bind(Properties bindings, Row row, int rowIndex, String columnName, Cell cell);
}
