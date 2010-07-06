package com.metaweb.gridworks.rdf.expr;

import java.util.Properties;

import com.metaweb.gridworks.expr.Binder;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.rdf.Util;

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
