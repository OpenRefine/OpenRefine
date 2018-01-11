package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.model.Cell;

public class WbStringVariable extends WbStringExpr {
    public static final String jsonType = "wbstringvariable";
    
    private String columnName;
    
    @JsonCreator
    public WbStringVariable(
            @JsonProperty("columnName") String columnName) {
        this.columnName = columnName;
    }

    @Override
    public StringValue evaluate(ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        Cell cell = ctxt.getCellByName(columnName);
        if (cell != null && !cell.value.toString().isEmpty()) {
            return Datamodel.makeStringValue(cell.value.toString());
        }
        throw new SkipSchemaExpressionException();
    }

    public String getColumnName() {
        return columnName;
    }
}
