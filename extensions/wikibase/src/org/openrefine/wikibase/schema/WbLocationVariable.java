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

import java.text.ParseException;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.schema.exceptions.SkipSchemaExpressionException;
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
        if (cell == null || cell.value == null) {
            throw new SkipSchemaExpressionException();
        }
        String expr = cell.value.toString();
        try {
            return WbLocationConstant.parse(expr);
        } catch (ParseException e) {
            if (!expr.trim().isEmpty()) {
                QAWarning issue = new QAWarning("ignored-coordinates", null, QAWarning.Severity.WARNING, 1);
                issue.setProperty("example_value", expr);
                ctxt.addWarning(issue);
            }
            throw new SkipSchemaExpressionException();
        }
    }

    @Override
    public boolean equals(Object other) {
        return equalAsVariables(other, WbLocationVariable.class);
    }
}
