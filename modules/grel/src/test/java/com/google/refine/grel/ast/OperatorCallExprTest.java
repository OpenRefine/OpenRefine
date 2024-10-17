
package com.google.refine.grel.ast;

import static org.testng.Assert.assertEquals;

import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.testng.annotations.Test;

public class OperatorCallExprTest extends ExprTestBase {

    private OperatorCallExpr expr;
    private GrelExpr arg1;
    private GrelExpr arg2;
    Object result;

    @Test
    public void evaluateDivision() {
        arg1 = new MockGrelExpr(10);
        arg2 = new MockGrelExpr(2);
        expr = new OperatorCallExpr(new GrelExpr[] { arg1, arg2 }, "/");
        result = expr.evaluate(new Properties());
        assertEquals((long) 5, result);
    }

    @Test
    public void evaluateZeroDivideZeroTest() {
        arg1 = new MockGrelExpr(0);
        arg2 = new MockGrelExpr(0);
        expr = new OperatorCallExpr(new GrelExpr[] { arg1, arg2 }, "/");
        result = expr.evaluate(new Properties());
        assertEquals(Double.NaN, result);
    }

    @Test
    public void evaluatePositiveIntegerDivideZeroTest() {
        arg1 = new MockGrelExpr(3);
        arg2 = new MockGrelExpr(0);
        expr = new OperatorCallExpr(new GrelExpr[] { arg1, arg2 }, "/");
        result = expr.evaluate(new Properties());
        assertEquals(Double.POSITIVE_INFINITY, result);
    }

    @Test
    public void evaluateNegativeIntegerDivideZeroTest() {
        arg1 = new MockGrelExpr(-3);
        arg2 = new MockGrelExpr(0);
        expr = new OperatorCallExpr(new GrelExpr[] { arg1, arg2 }, "/");
        result = expr.evaluate(new Properties());
        assertEquals(Double.NEGATIVE_INFINITY, result);
    }

    @Test
    public void testUnion() {
        GrelExpr ev = new OperatorCallExpr(new GrelExpr[] { constant, currentColumn, twoColumns }, "+");
        assertEquals(ev.getColumnDependencies(baseColumn), set("baseColumn", "a", "b"));
    }

    @Test
    public void testUnanalyzable() {
        GrelExpr ev = new OperatorCallExpr(new GrelExpr[] { currentColumn, unanalyzable }, "+");
        assertEquals(ev.getColumnDependencies(baseColumn), Optional.empty());
    }
}

class MockGrelExpr implements GrelExpr {

    private final Object value;

    public MockGrelExpr(Object value) {
        this.value = value;
    }

    @Override
    public Object evaluate(Properties bindings) {
        return value;
    }

    @Override
    public Optional<Set<String>> getColumnDependencies(Optional<String> baseColumn) {
        return Optional.empty();
    }
}
