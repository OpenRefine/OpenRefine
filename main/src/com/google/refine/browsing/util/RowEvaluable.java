package com.google.refine.browsing.util;

import java.util.Properties;

import com.google.refine.model.Project;
import com.google.refine.model.Row;

public interface RowEvaluable {
    public Object eval(Project project, int rowIndex, Row row, Properties bindings);
}
