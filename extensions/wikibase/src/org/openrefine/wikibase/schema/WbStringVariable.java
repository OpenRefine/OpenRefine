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

import org.openrefine.wikibase.schema.exceptions.SkipSchemaExpressionException;
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
     * Constructs a variable and sets the column it is bound to. Mostly used as a convenience method for testing.
     * 
     * @param columnName
     *            the name of the column the expression should draw its value from
     */
    public WbStringVariable(String columnName) {
        setColumnName(columnName);
    }

    @Override
    public StringValue fromCell(Cell cell, ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        if (cell != null && cell.value != null && !cell.value.toString().isEmpty()) {
            String stringValue = cell.value.toString();
            if (cell.value instanceof Double && ((Double) cell.value) % 1 == 0) {
                stringValue = Long.toString(((Double) cell.value).longValue());
            }
            return Datamodel.makeStringValue(stringValue.trim());
        }
        throw new SkipSchemaExpressionException();
    }

    @Override
    public boolean equals(Object other) {
        return equalAsVariables(other, WbStringVariable.class);
    }
}
