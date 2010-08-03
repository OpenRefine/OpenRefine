package com.google.gridworks.rdf.expr;

import java.util.Properties;

import com.google.gridworks.expr.Binder;
import com.google.gridworks.model.Cell;
import com.google.gridworks.model.Project;
import com.google.gridworks.model.Row;
import com.google.gridworks.rdf.Util;

public class RdfBinder implements Binder {

    @Override
    public void bind(Properties bindings, Row row, int rowIndex, String columnName, Cell cell) {
        // nothing to do
    }

    @Override
    public void initializeBindings(Properties bindings, Project project) {
        bindings.put("baseURI", Util.getProjectSchema(project).getBaseUri());
    }

}
