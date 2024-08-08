
package com.google.refine.grel.ast;

import static org.testng.Assert.assertEquals;

import java.util.Optional;
import java.util.Properties;

import org.testng.annotations.Test;

import com.google.refine.expr.Evaluable;

public class OperatorCallExprTest extends ExprTestBase {

    private OperatorCallExpr expr;
    private Evaluable arg1;
    private Evaluable arg2;
    Object result;

    @Test
    public void evaluateDivision() {
        arg1 = new MockEvaluable(10);
        arg2 = new MockEvaluable(2);
        expr = new OperatorCallExpr(new Evaluable[] { arg1, arg2 }, "/");
        result = expr.evaluate(new Properties());
        assertEquals((long) 5, result);
    }

    @Test
    public void evaluateZeroDivideZeroTest() {
        arg1 = new MockEvaluable(0);
        arg2 = new MockEvaluable(0);
        expr = new OperatorCallExpr(new Evaluable[] { arg1, arg2 }, "/");
        result = expr.evaluate(new Properties());
        assertEquals(Double.NaN, result);
    }

    @Test
    public void evaluatePositiveIntegerDivideZeroTest() {
        arg1 = new MockEvaluable(3);
        arg2 = new MockEvaluable(0);
        expr = new OperatorCallExpr(new Evaluable[] { arg1, arg2 }, "/");
        result = expr.evaluate(new Properties());
        assertEquals(Double.POSITIVE_INFINITY, result);
    }

    @Test
    public void evaluateNegativeIntegerDivideZeroTest() {
        arg1 = new MockEvaluable(-3);
        arg2 = new MockEvaluable(0);
        expr = new OperatorCallExpr(new Evaluable[] { arg1, arg2 }, "/");
        result = expr.evaluate(new Properties());
        assertEquals(Double.NEGATIVE_INFINITY, result);
    }

    @Test
    public void testUnion() {
        Evaluable ev = new OperatorCallExpr(new Evaluable[] { constant, currentColumn, twoColumns }, "+");
        assertEquals(ev.getColumnDependencies(baseColumn), set("baseColumn", "a", "b"));
    }

    @Test
    public void testUnanalyzable() {
        Evaluable ev = new OperatorCallExpr(new Evaluable[] { currentColumn, unanalyzable }, "+");
        assertEquals(ev.getColumnDependencies(baseColumn), Optional.empty());
    }
}

class MockEvaluable implements Evaluable {

    private final Object value;

    public MockEvaluable(Object value) {
        this.value = value;
    }

    @Override
    public Object evaluate(Properties bindings) {
        return value;
    }
}
