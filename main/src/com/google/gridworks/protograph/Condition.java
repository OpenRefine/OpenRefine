package com.google.gridworks.protograph;

import com.google.gridworks.Jsonizable;
import com.google.gridworks.model.Project;
import com.google.gridworks.model.Row;

public interface Condition extends Jsonizable {
    public boolean test(Project project, int rowIndex, Row row);
}
