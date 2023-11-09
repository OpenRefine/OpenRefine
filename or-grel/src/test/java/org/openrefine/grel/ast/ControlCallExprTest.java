
package org.openrefine.grel.ast;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.expr.Evaluable;
import org.openrefine.grel.Control;

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
        assertEquals(c.getColumnDependencies(baseColumn), set("a", "b", baseColumn));
    }

    @Test
    public void testUnanalyzable() {
        Evaluable c = new ControlCallExpr(new Evaluable[] { twoColumns, unanalyzable }, control);
        assertNull(c.getColumnDependencies(baseColumn));
    }
}
