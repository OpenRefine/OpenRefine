package com.google.refine.grel.ast;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.expr.Evaluable;
import com.google.refine.grel.Function;
import com.google.refine.grel.PureFunction;

public class FunctionCallExprTest extends ExprTestBase {
    
    protected PureFunction function;
    
    @BeforeTest
    public void setUpFunction() {
        function = mock(PureFunction.class);
    }
    
    @Test
    public void testUnion() {
        Evaluable ev = new FunctionCallExpr(new Evaluable[] {constant,currentColumn,twoColumns}, function);
        assertEquals(ev.getColumnDependencies(baseColumn), set(baseColumn, "a", "b"));
    }
    
    @Test
    public void testUnanalyzable() {
        Evaluable ev = new FunctionCallExpr(new Evaluable []{currentColumn,unanalyzable}, function);
        assertNull(ev.getColumnDependencies(baseColumn));
    }
    
    @Test
    public void testImpureFunction() {
        Evaluable ev = new FunctionCallExpr(
                new Evaluable []{currentColumn,constant},
                      mock(Function.class));
        assertNull(ev.getColumnDependencies(baseColumn));
    }

}
