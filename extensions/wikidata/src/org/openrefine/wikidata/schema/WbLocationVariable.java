package org.openrefine.wikidata.schema;

import java.text.ParseException;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.google.refine.model.Cell;


public class WbLocationVariable extends WbVariableExpr<GlobeCoordinatesValue> {
    
    @JsonCreator
    public WbLocationVariable() {
        
    }

    public WbLocationVariable(String columnName) {
        setColumnName(columnName);
    }

    @Override
    public GlobeCoordinatesValue fromCell(Cell cell, ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        String expr = cell.value.toString();
        try {
            return WbLocationConstant.parse(expr);
        } catch (ParseException e) {
            throw new SkipSchemaExpressionException();
        }
    }
    
    @Override
    public boolean equals(Object other) {
        return equalAsVariables(other, WbLocationVariable.class);
    }
}
