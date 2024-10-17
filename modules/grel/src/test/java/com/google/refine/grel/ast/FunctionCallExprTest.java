
package com.google.refine.grel.ast;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.grel.Function;

public class FunctionCallExprTest extends ExprTestBase {

    protected Function function;

    @BeforeTest
    public void setUpFunction() {
        function = mock(Function.class);
    }

    @Test
    public void testUnion() {
        GrelExpr ev = new FunctionCallExpr(new GrelExpr[] { constant, currentColumn, twoColumns }, function);
        assertEquals(ev.getColumnDependencies(baseColumn), set("baseColumn", "a", "b"));
    }

    @Test
    public void testUnanalyzable() {
        GrelExpr ev = new FunctionCallExpr(new GrelExpr[] { currentColumn, unanalyzable }, function);
        assertEquals(ev.getColumnDependencies(baseColumn), Optional.empty());
    }
}
