
package org.openrefine.grel.ast;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.grel.Function;
import org.openrefine.grel.PureFunction;
import org.openrefine.grel.ast.FunctionCallExpr;

public class FunctionCallExprTest extends ExprTestBase {

    protected PureFunction function;

    @BeforeTest
    public void setUpFunction() {
        function = mock(PureFunction.class);
    }

    @Test
    public void testUnion() {
        GrelExpr ev = new FunctionCallExpr(new GrelExpr[] { constant, currentColumn, twoColumns }, function, "foo");
        assertEquals(ev.getColumnDependencies(baseColumn), set(baseColumn, "a", "b"));
        assertTrue(ev.isLocal());
    }

    @Test
    public void testUnanalyzable() {
        GrelExpr ev = new FunctionCallExpr(new GrelExpr[] { currentColumn, unanalyzable }, function, "foo");
        assertNull(ev.getColumnDependencies(baseColumn));
        assertFalse(ev.isLocal());
    }

    @Test
    public void testImpureFunction() {
        GrelExpr ev = new FunctionCallExpr(
                new GrelExpr[] { currentColumn, constant },
                mock(Function.class), "foo");
        assertNull(ev.getColumnDependencies(baseColumn));
        assertFalse(ev.isLocal());
    }

    @Test
    public void testToString() {
        GrelExpr arg = mock(GrelExpr.class);
        when(arg.toString()).thenReturn("arg");
        GrelExpr SUT = new FunctionCallExpr(new GrelExpr[] { arg }, function, "myFunction");
        assertEquals(SUT.toString(), "myFunction(arg)");
    }
}
