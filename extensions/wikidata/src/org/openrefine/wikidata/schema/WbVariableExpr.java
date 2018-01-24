package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.model.Cell;

/**
 * A base class for expressions which draw their values
 * from a particular column.
 * 
 * @author antonin
 *
 * @param <T>
 *      the type of Wikibase value returned by the expression.
 */
public abstract class WbVariableExpr<T> implements WbExpression<T> {

    private String columnName;
    
    /**
     * Constructs a variable expression from a column name.
     * 
     * @param columnName
     *     the name of the column the expression should draw its value from.
     */
    @JsonCreator
    public WbVariableExpr(
            @JsonProperty("columnName") String columnName) {
        this.columnName = columnName;
    }
    
    /**
     * Constructs a variable without setting the column name yet.
     */
    @JsonCreator
    public WbVariableExpr() {
        columnName = null;
    }
    
    /**
     * Returns the column name used by the variable.
     * @return
     *          the OpenRefine column name
     */
    @JsonProperty("columnName")
    public String getColumnName() {
        return columnName;
    }
    
    /**
     * Changes the column name used by the variable.
     * This is useful for deserialization, as well as updates when
     * column names change.
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
     * Method that should be implemented by subclasses,
     * converting an OpenRefine cell to a Wikibase value.
     * Access to other values and emiting warnings is possible via
     * the supplied EvaluationContext object.
     * 
     * @param cell
     *          the cell to convert
     * @param ctxt
     *          the evaluation context
     * @return
     *          the corresponding Wikibase value
     */
    public abstract T fromCell(Cell cell, ExpressionContext ctxt) throws SkipSchemaExpressionException;
}
