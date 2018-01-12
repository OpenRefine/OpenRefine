package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.model.Cell;


public class WbLanguageVariable extends WbLanguageExpr {
    
    private String columnName;
    
    @JsonCreator
    public WbLanguageVariable(
            @JsonProperty("columnName") String columnName) {
        this.columnName = columnName;
    }
    
    @Override
    public String evaluate(ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        Cell cell = ctxt.getCellByName(getColumnName());
        if (cell != null && cell.value != null && !cell.value.toString().isEmpty()) {
            // TODO some validation here?
            return cell.value.toString();
        }
        throw new SkipSchemaExpressionException();
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return columnName;
    }

}
