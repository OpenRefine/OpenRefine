package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.interfaces.WikimediaLanguageCodes;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.google.refine.model.Cell;

/**
 * A language variable generates a language code from a cell.
 * It checks its values against a known list of valid language codes
 * and fixes on the fly the deprecated ones (see {@link WbLanguageConstant}).
 */
public class WbLanguageVariable extends WbVariableExpr<String> {
    
    @JsonCreator
    public WbLanguageVariable() {  
    }

    /**
     * Constructs a variable and sets the column it is bound to. Mostly
     * used as a convenience method for testing.
     * 
     * @param columnName
     *     the name of the column the expression should draw its value from
     */
    public WbLanguageVariable(String columnName) {
        setColumnName(columnName);
    }

    @Override
    public String fromCell(Cell cell, ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        if (cell.value != null && !cell.value.toString().isEmpty()) {
            String code = cell.value.toString().trim();
            String normalized = WbLanguageConstant.normalizeLanguageCode(code);
            if (normalized != null) {
                return normalized;
            }
        }
        throw new SkipSchemaExpressionException();
    }
    
    @Override
    public boolean equals(Object other) {
        return equalAsVariables(other, WbLanguageVariable.class);
    }
}
