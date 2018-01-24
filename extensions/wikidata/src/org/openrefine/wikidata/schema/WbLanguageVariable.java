package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;

import com.google.refine.model.Cell;

/**
 * A language variable generates a language code from a cell.
 */
public class WbLanguageVariable extends WbVariableExpr<String> {

    @Override
    public String fromCell(Cell cell, ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        if (cell.value != null && !cell.value.toString().isEmpty()) {
            // TODO some validation here?
            return cell.value.toString();
        }
        throw new SkipSchemaExpressionException();
    }
}
