package com.google.refine.expr;

import java.util.Properties;

import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public interface Binder {
    public void initializeBindings(Properties bindings, Project project);
        
    public void bind(Properties bindings, Row row, int rowIndex, String columnName, Cell cell);
}
