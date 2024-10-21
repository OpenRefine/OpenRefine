
package com.google.refine.grel;

import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import com.google.refine.expr.Evaluable;
import com.google.refine.grel.ast.GrelExpr;

/**
 * An evaluable made out of a GREL expression.
 */
public class GrelEvaluable implements Evaluable {

    private GrelExpr _expr;

    public GrelEvaluable(GrelExpr expr) {
        _expr = expr;
    }

    @Override
    public Object evaluate(Properties bindings) {
        return _expr.evaluate(bindings);
    }

    @Override
    public Optional<Set<String>> getColumnDependencies(Optional<String> baseColumn) {
        return _expr.getColumnDependencies(baseColumn);
    }

}
