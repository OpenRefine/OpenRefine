package com.google.gridworks.browsing.util;

import java.util.Properties;

import com.google.gridworks.model.Project;
import com.google.gridworks.model.Row;

public interface RowEvaluable {
    public Object eval(Project project, int rowIndex, Row row, Properties bindings);
}
