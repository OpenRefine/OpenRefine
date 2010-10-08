package com.google.refine.freebase.protograph;

import com.google.refine.Jsonizable;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public interface Condition extends Jsonizable {
    public boolean test(Project project, int rowIndex, Row row);
}
