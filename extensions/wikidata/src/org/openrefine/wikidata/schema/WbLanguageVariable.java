package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.interfaces.WikimediaLanguageCodes;

import com.google.refine.model.Cell;

/**
 * A language variable generates a language code from a cell.
 */
public class WbLanguageVariable extends WbVariableExpr<String> {

    @Override
    public String fromCell(Cell cell, ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        if (cell.value != null && !cell.value.toString().isEmpty()) {
            String code = cell.value.toString().trim();
            try {
                // this just checks that the language code is known
                WikimediaLanguageCodes.getLanguageCode(code);
                return cell.value.toString();
            } catch(IllegalArgumentException e) {
                ;
            }
        }
        throw new SkipSchemaExpressionException();
    }
}
