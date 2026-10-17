package com.google.refine.grel.controls;

import java.util.Properties;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.grel.ast.LiteralExpr;

public class AllTest {
    Properties bindings;

    @BeforeMethod
    public void setUp() {
        bindings = new Properties();
    }

    @Test
    public void testAllWithFalse() {
        All all = new All();
        Evaluable[] args = new Evaluable[] {
            new LiteralExpr(new Boolean[] { true, false, true }, "array")
        };
        Assert.assertFalse((Boolean) all.call(bindings, args));
    }

    @Test
    public void testAllAllTrue() {
        All all = new All();
        Evaluable[] args = new Evaluable[] {
            new LiteralExpr(new Boolean[] { true, true }, "array")
        };
        Assert.assertTrue((Boolean) all.call(bindings, args));
    }

    @Test
    public void testAllEmpty() {
        All all = new All();
        Evaluable[] args = new Evaluable[] {
            new LiteralExpr(new Object[] {}, "array")
        };
        Assert.assertTrue((Boolean) all.call(bindings, args)); 
    }

    @Test
    public void testAllNotArray() {
        All all = new All();
        Evaluable[] args = new Evaluable[] {
            new LiteralExpr("not an array", "string")
        };
        Assert.assertTrue(all.call(bindings, args) instanceof EvalError);
    }
}