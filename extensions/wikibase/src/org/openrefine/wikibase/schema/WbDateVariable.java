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
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.refine.model.Cell;

/**
 * An expression that represents a time value, extracted from a string. A number of formats are recognized, see
 * {@link WbDateConstant} for details.
 * 
 * @author Antonin Delpeuch
 *
 */
public class WbDateVariable extends WbVariableExpr<TimeValue> {

    @JsonCreator
    public WbDateVariable() {

    }

    public WbDateVariable(String columnName) {
        setColumnName(columnName);
    }

    @Override
    public TimeValue fromCell(Cell cell, ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        if (cell == null || cell.value == null) {
            throw new SkipSchemaExpressionException();
        }
        try {
            // parsed dates are accepted by converting them to strings
            return WbDateConstant.parse(cell.value.toString());
        } catch (ParseException e) {
            if (!cell.value.toString().isEmpty()) {
                QAWarning issue = new QAWarning("ignored-date", null, QAWarning.Severity.WARNING, 1);
                issue.setProperty("example_value", cell.value.toString());
                ctxt.addWarning(issue);
            }
            throw new SkipSchemaExpressionException();
        }
    }

    @Override
    public boolean equals(Object other) {
        return equalAsVariables(other, WbDateVariable.class);
    }
}
