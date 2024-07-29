
package com.google.refine.grel.ast;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.expr.Evaluable;
import com.google.refine.grel.Control;

public class ControlCallExprTest extends ExprTestBase {

    Control control;

    @BeforeMethod
    public void setUpControl() {
        control = mock(Control.class);
    }

    @Test
    public void testConstant() {
        Evaluable c = new ControlCallExpr(new Evaluable[] { constant }, control);
        assertEquals(c.getColumnDependencies(baseColumn), set());
    }

    @Test
    public void testUnion() {
        Evaluable c = new ControlCallExpr(new Evaluable[] { twoColumns, currentColumn }, control);
        assertEquals(c.getColumnDependencies(baseColumn), set("a", "b", "baseColumn"));
    }

    @Test
    public void testUnanalyzable() {
        Evaluable c = new ControlCallExpr(new Evaluable[] { twoColumns, unanalyzable }, control);
        assertEquals(c.getColumnDependencies(baseColumn), Optional.empty());
    }
}
