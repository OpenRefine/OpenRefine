package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.google.refine.model.Cell;

/**
 * A variable that returns a simple string value.
 * 
 * @author Antonin Delpeuch
 *
 */
public class WbStringVariable extends WbVariableExpr<StringValue> {
    
    @JsonCreator
    public WbStringVariable() {  
    }

    /**
     * Constructs a variable and sets the column it is bound to. Mostly
     * used as a convenience method for testing.
     * 
     * @param columnName
     *     the name of the column the expression should draw its value from
     */
    public WbStringVariable(String columnName) {
        setColumnName(columnName);
    }

    @Override
    public StringValue fromCell(Cell cell, ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        if (!cell.value.toString().isEmpty()) {
            return Datamodel.makeStringValue(cell.value.toString());
        }
        throw new SkipSchemaExpressionException();
    }
    
    @Override
    public boolean equals(Object other) {
        return equalAsVariables(other, WbStringVariable.class);
    }
}
