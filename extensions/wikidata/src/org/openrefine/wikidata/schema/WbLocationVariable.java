package org.openrefine.wikidata.schema;

import java.text.ParseException;

import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.model.Cell;


public class WbLocationVariable extends WbLocationExpr {
    
    private String columnName;
    
    @JsonCreator
    public WbLocationVariable(
            @JsonProperty("columnName") String columnName) {
        this.columnName = columnName;
    }

    @Override
    public GlobeCoordinatesValue evaluate(ExpressionContext ctxt)
            throws SkipStatementException {
        Cell cell = ctxt.getCellByName(getColumnName());
        if (cell != null) {
            String expr = cell.value.toString();
            try {
                return WbLocationConstant.parse(expr);
            } catch (ParseException e) {
            }
        }
        throw new SkipStatementException();
    }

    public String getColumnName() {
        return columnName;
    }
}
