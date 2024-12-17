
package com.google.refine.grel.ast;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import com.google.refine.expr.Evaluable;

/**
 * An AST node which represents a bracketed expression. This is introduced to enable faithful printing of a parsed
 * expression, avoiding operator precedence issues for instance.
 */
public class BracketedExpr extends GrelExpr {

    protected final Evaluable inner;

    /**
     * @param inner
     *            the expression inside the brackets
     */
    public BracketedExpr(Evaluable inner) {
        this.inner = inner;
    }

    @Override
    public Object evaluate(Properties bindings) {
        return inner.evaluate(bindings);
    }

    @Override
    public Optional<Set<String>> getColumnDependencies(Optional<String> baseColumn) {
        return inner.getColumnDependencies(baseColumn);
    }

    @Override
    public String toString() {
        return "(" + inner + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(inner);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BracketedExpr other = (BracketedExpr) obj;
        return Objects.equals(inner, other.inner);
    }

}
