
package org.openrefine.grel.ast;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.grel.Control;

public class ControlCallExprTest extends ExprTestBase {

    Control control;

    @BeforeMethod
    public void setUpControl() {
        control = mock(Control.class);
    }

    @Test
    public void testConstant() {
        GrelExpr c = new ControlCallExpr(new GrelExpr[] { constant }, control);
        assertEquals(c.getColumnDependencies(baseColumn), set());
    }

    @Test
    public void testUnion() {
        GrelExpr c = new ControlCallExpr(new GrelExpr[] { twoColumns, currentColumn }, control);
        assertEquals(c.getColumnDependencies(baseColumn), set("a", "b", baseColumn));
    }

    @Test
    public void testUnanalyzable() {
        GrelExpr c = new ControlCallExpr(new GrelExpr[] { twoColumns, unanalyzable }, control);
        assertNull(c.getColumnDependencies(baseColumn));
    }
}
