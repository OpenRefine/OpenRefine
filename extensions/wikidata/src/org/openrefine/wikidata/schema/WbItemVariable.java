package org.openrefine.wikidata.schema;


import org.openrefine.wikidata.schema.entityvalues.ReconItemIdValue;
import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.google.refine.model.Cell;
import com.google.refine.model.Recon.Judgment;

/**
 * An item that depends on a reconciled value in a column.
 * 
 * @author Antonin Delpeuch
 *
 */
public class WbItemVariable extends WbVariableExpr<ItemIdValue> {
    
    @JsonCreator
    public WbItemVariable() {
        
    }

    /**
     * Constructs a variable and sets the column it is bound to. Mostly
     * used as a convenience method for testing.
     * 
     * @param columnName
     *     the name of the column the expression should draw its value from
     */
    public WbItemVariable(String columnName) {
        setColumnName(columnName);
    }

    @Override
    public ItemIdValue fromCell(Cell cell, ExpressionContext ctxt) throws SkipSchemaExpressionException {
        if (cell.recon != null
                && (Judgment.Matched.equals(cell.recon.judgment) ||
                    Judgment.New.equals(cell.recon.judgment))) {
            return new ReconItemIdValue(cell.recon, cell.value.toString());
        }
        throw new SkipSchemaExpressionException();
    }
    
    @Override
    public boolean equals(Object other) {
        return equalAsVariables(other, WbItemVariable.class);
    }
}
