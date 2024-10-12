
package com.google.refine.grel.ast;

import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * A GREL expression, which can be evaluated in a given context.
 * <p>
 * This is introduced to serve as base class for all GREL expressions, instead of
 * {@link com.google.refine.expr.Evaluable} which is the base class for all evaluables in OpenRefine.
 */
public interface GrelExpr {

    /**
     * Returns the value of the expression in a given context.
     * 
     * @param bindings
     *            the evaluation context, mapping variable names to their values
     * @return the result of the evaluation of the expression
     */
    public Object evaluate(Properties bindings);

    /**
     * For GREL expressions, toString should return the source code of the expression (without the "grel:" prefix).
     */
    public String toString();

    /**
     * Returns an approximation of the names of the columns this expression depends on. This approximation is designed
     * to be safe: if a set of column names is returned, then the expression does not read any other column than the
     * ones mentioned, regardless of the data it is executed on.
     *
     * @param baseColumn
     *            the name of the column this expression is based on (none if the expression is not evaluated on a
     *            particular column)
     * @return {@link Optional#empty()} if the columns could not be isolated: in this case, the expression might depend
     *         on all columns in the project. Note that this is different from returning an empty set, which means that
     *         the expression is constant.
     */
    public Optional<Set<String>> getColumnDependencies(Optional<String> baseColumn);
}
