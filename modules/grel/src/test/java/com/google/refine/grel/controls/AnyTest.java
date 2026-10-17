package com.google.refine.grel.controls;

import java.util.Properties;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.grel.ast.LiteralExpr;

public class AnyTest {
    Properties bindings;

    @BeforeMethod
    public void setUp() {
        bindings = new Properties();
    }

    @Test
    public void testAnyAllFalse() {
        Any any = new Any();
        Evaluable[] args = new Evaluable[] {
            new LiteralExpr(new Boolean[] { false, false }, "array")
        };
        Assert.assertFalse((Boolean) any.call(bindings, args));
    }

    @Test
    public void testAnyWithTrue() {
        Any any = new Any();
        Evaluable[] args = new Evaluable[] {
            new LiteralExpr(new Boolean[] { false, true, false }, "array")
        };
        Assert.assertTrue((Boolean) any.call(bindings, args));
    }

    @Test
    public void testAnyEmpty() {
        Any any = new Any();
        Evaluable[] args = new Evaluable[] {
            new LiteralExpr(new Object[] {}, "array")
        };
        Assert.assertFalse((Boolean) any.call(bindings, args));
    }
    
    @Test
    public void testAnyNotArray() {
        Any any = new Any();
        Evaluable[] args = new Evaluable[] {
            new LiteralExpr("not an array", "string")
        };
        Assert.assertTrue(any.call(bindings, args) instanceof EvalError);
    }
}