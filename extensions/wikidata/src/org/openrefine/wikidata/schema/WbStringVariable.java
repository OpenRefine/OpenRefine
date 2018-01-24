package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

import com.google.refine.model.Cell;

/**
 * A variable that returns a simple string value.
 * 
 * @author antonin
 *
 */
public class WbStringVariable extends WbVariableExpr<StringValue> {

    @Override
    public StringValue fromCell(Cell cell, ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        if (!cell.value.toString().isEmpty()) {
            return Datamodel.makeStringValue(cell.value.toString());
        }
        throw new SkipSchemaExpressionException();
    }
}
