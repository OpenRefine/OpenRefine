package org.openrefine.wikidata.schema;

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
            throws SkipStatementException {
        Cell cell = ctxt.getCellByName(getColumnName());
        if (cell != null) {
            // TODO some validation here?
            return cell.value.toString();
        }
        throw new SkipStatementException();
    }

    public String getColumnName() {
        return columnName;
    }

}
