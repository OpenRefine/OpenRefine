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
package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.model.Cell;

/**
 * A base class for expressions which draw their values from a particular
 * column.
 * 
 * @author Antonin Delpeuch
 *
 * @param <T>
 *            the type of Wikibase value returned by the expression.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class WbVariableExpr<T> implements WbExpression<T> {

    private String columnName;

    /**
     * Constructs a variable without setting the column name yet.
     */
    @JsonCreator
    public WbVariableExpr() {
        columnName = null;
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
     * Changes the column name used by the variable. This is useful for
     * deserialization, as well as updates when column names change.
     */
    @JsonProperty("columnName")
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    /**
     * Evaluates the expression in a given context, returning
     */
    @Override
    public T evaluate(ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        Cell cell = ctxt.getCellByName(columnName);
        if (cell != null) {
            return fromCell(cell, ctxt);
        }
        throw new SkipSchemaExpressionException();
    }

    /**
     * Method that should be implemented by subclasses, converting an OpenRefine
     * cell to a Wikibase value. Access to other values and emiting warnings is
     * possible via the supplied EvaluationContext object.
     * 
     * @param cell
     *            the cell to convert
     * @param ctxt
     *            the evaluation context
     * @return the corresponding Wikibase value
     */
    public abstract T fromCell(Cell cell, ExpressionContext ctxt)
            throws SkipSchemaExpressionException;

    /**
     * Helper for equality methods of subclasses.
     * 
     * @param other
     *            the object to compare
     * @param columnName
     *            the column name to compare to
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
