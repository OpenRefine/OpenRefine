
package org.openrefine.grel.ast;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.grel.Control;
import org.openrefine.grel.ast.ControlCallExpr;

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
        assertTrue(c.isLocal());
    }

    @Test
    public void testUnion() {
        GrelExpr c = new ControlCallExpr(new GrelExpr[] { twoColumns, currentColumn }, control);
        assertEquals(c.getColumnDependencies(baseColumn), set("a", "b", baseColumn));
        assertTrue(c.isLocal());
    }

    @Test
    public void testUnanalyzable() {
        GrelExpr c = new ControlCallExpr(new GrelExpr[] { twoColumns, unanalyzable }, control);
        assertNull(c.getColumnDependencies(baseColumn));
        assertFalse(c.isLocal());
    }
}
