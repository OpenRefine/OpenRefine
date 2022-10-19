/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package org.openrefine.wikibase.schema;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.schema.exceptions.SkipSchemaExpressionException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.refine.model.Cell;

/**
 * A language variable generates a language code from a cell. It checks its values against a known list of valid
 * language codes and fixes on the fly the deprecated ones (see {@link WbLanguageConstant}).
 */
public class WbLanguageVariable extends WbVariableExpr<String> {

    @JsonCreator
    public WbLanguageVariable() {
    }

    /**
     * Constructs a variable and sets the column it is bound to. Mostly used as a convenience method for testing.
     * 
     * @param columnName
     *            the name of the column the expression should draw its value from
     */
    public WbLanguageVariable(String columnName) {
        setColumnName(columnName);
    }

    @Override
    public String fromCell(Cell cell, ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        if (cell.value != null && !cell.value.toString().isEmpty()) {
            String code = cell.value.toString().trim();
            String mediaWikiApiEndpoint = ctxt.getMediaWikiApiEndpoint();
            String normalized = WbLanguageConstant.normalizeLanguageCode(code, mediaWikiApiEndpoint);
            if (normalized != null) {
                return normalized;
            } else {
                QAWarning issue = new QAWarning("ignored-language", null, QAWarning.Severity.WARNING, 1);
                issue.setProperty("example_value", cell.value.toString());
                ctxt.addWarning(issue);
            }
        }
        throw new SkipSchemaExpressionException();
    }

    @Override
    public boolean equals(Object other) {
        return equalAsVariables(other, WbLanguageVariable.class);
    }
}
