package org.openrefine.wikidata.schema;

import java.text.ParseException;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

import com.google.refine.model.Cell;

/**
 * An expression that represents a time value, extracted from a string.
 * A number of formats are recognized, see {@link WbDateConstant} for details.
 * 
 * @author Antonin Delpeuch
 *
 */
public class WbDateVariable extends WbVariableExpr<TimeValue> {

    @Override
    public TimeValue fromCell(Cell cell, ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        try {
            // TODO accept parsed dates (without converting them to strings)
            return WbDateConstant.parse(cell.value.toString());
        } catch (ParseException e) {
            throw new SkipSchemaExpressionException();
        }
    }
    
    @Override
    public boolean equals(Object other) {
        return equalAsVariables(other, WbDateVariable.class);
    }
}
