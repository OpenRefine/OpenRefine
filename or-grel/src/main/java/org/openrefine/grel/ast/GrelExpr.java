
package org.openrefine.grel.ast;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * A GREL expression, which can be evaluated in a given context.
 * 
 * This is introduced to serve as base class for all GREL expressions, instead of {@class org.openrefine.expr.Evaluable}
 * which is the base class for all evaluables in OpenRefine.
 * 
 * @author Antonin Delpeuch
 */
public interface GrelExpr extends Serializable {

    /**
     * Returns the value of the expression in a given context.
     * 
     * @param bindings
     *            the evaluation context, mapping variable names to their values
     * @return the result of the evaluation of the expression
     */
    public Object evaluate(Properties bindings);

    /**
     * For GREL expressions, toString should return the source code of the expression, or a source code for an
     * equivalent expression. (without the "grel:" prefix).
     */
    public String toString();

    /**
     * Returns the names of the columns this expression depends on.
     * 
     * @param baseColumn
     *            the name of the column this expression is based on (null if none)
     * @returns null if the columns could not be isolated: in this case, the expression might depend on all columns in
     *          the project.
     */
    public Set<String> getColumnDependencies(String baseColumn);

    /**
     * Translates this expression by simultaneously substituting column names as the supplied map specifies.
     * 
     * This is only possible if the extraction of column dependencies with {@link #getColumnDependencies(String)}
     * succeeds (return a non-null value).
     * 
     * @param substitutions
     *            a map specifying new names for some columns. If a column name is not present in the map, it is assumed
     *            that the column is not renamed.
     * @return a new expression with updated column names.
     */
    public GrelExpr renameColumnDependencies(Map<String, String> substitutions);
}
