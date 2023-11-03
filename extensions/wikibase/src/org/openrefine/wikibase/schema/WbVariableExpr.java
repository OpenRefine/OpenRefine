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

import org.openrefine.wikibase.schema.exceptions.QAWarningException;
import org.openrefine.wikibase.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikibase.schema.exceptions.SpecialValueNoValueException;
import org.openrefine.wikibase.schema.exceptions.SpecialValueSomeValueException;
import org.openrefine.wikibase.schema.validation.ValidationState;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.model.Cell;

/**
 * A base class for expressions which draw their values from a particular column.
 * 
 * @author Antonin Delpeuch
 *
 * @param <T>
 *            the type of Wikibase value returned by the expression.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class WbVariableExpr<T> implements WbExpression<T> {

    public static final String NO_VALUE_KEYWORD = "#NOVALUE#";
    public static final String SOME_VALUE_KEYWORD = "#SOMEVALUE#";

    private String columnName;

    /**
     * Constructs a variable without setting the column name yet.
     */
    @JsonCreator
    public WbVariableExpr() {
        columnName = null;
    }

    /**
     * Checks that we have a valid column name.
     */
    @Override
    public void validate(ValidationState validation) {
        if (columnName == null) {
            validation.addError("No column provided");
        }
        if (validation.getColumnModel().getColumnByName(columnName) == null) {
            validation.addError("Column '" + columnName + "' does not exist");
        }
    }

    /**
     * Returns the column name used by the variable.
     * 
     * @return the OpenRefine column name
     */
    @JsonProperty("columnName")
    public String getColumnName() {
        return columnName;
    }

    /**
     * Changes the column name used by the variable. This is useful for deserialization, as well as updates when column
     * names change.
     */
    @JsonProperty("columnName")
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    /**
     * Evaluates the expression in a given context, returning
     * 
     * @throws QAWarningException
     * @throws SpecialValueNoValueException
     * @throws SpecialValueSomeValueException
     */
    @Override
    public T evaluate(ExpressionContext ctxt)
            throws SkipSchemaExpressionException, QAWarningException, SpecialValueNoValueException, SpecialValueSomeValueException {
        Cell cell = ctxt.getCellByName(columnName);
        if (cell != null) {
            if (NO_VALUE_KEYWORD.equals(cell.toString())) {
                throw new SpecialValueNoValueException();
            } else if (SOME_VALUE_KEYWORD.equals(cell.toString())) {
                throw new SpecialValueSomeValueException();
            }

            return fromCell(cell, ctxt);
        }
        throw new SkipSchemaExpressionException();
    }

    /**
     * Method that should be implemented by subclasses, converting an OpenRefine cell to a Wikibase value. Access to
     * other values and emitting warnings is possible via the supplied EvaluationContext object.
     * 
     * @param cell
     *            the cell to convert
     * @param ctxt
     *            the evaluation context
     * @return the corresponding Wikibase value
     */
    public abstract T fromCell(Cell cell, ExpressionContext ctxt)
            throws SkipSchemaExpressionException, QAWarningException;

    /**
     * Helper for equality methods of subclasses.
     * 
     * @param other
     *            the object to compare
     * @param targetClass
     *            the target class for equality
     * @return
     */
    protected boolean equalAsVariables(Object other, Class<? extends WbVariableExpr<?>> targetClass) {
        if (other == null || !targetClass.isInstance(other)) {
            return false;
        }
        return columnName.equals(targetClass.cast(other).getColumnName());
    }

    @Override
    public int hashCode() {
        return columnName.hashCode();
    }

}
