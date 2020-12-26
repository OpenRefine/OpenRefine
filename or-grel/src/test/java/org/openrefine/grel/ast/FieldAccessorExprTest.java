
package org.openrefine.grel.ast;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

import org.openrefine.grel.ast.FieldAccessorExpr;

public class FieldAccessorExprTest extends ExprTestBase {

    @Test
    public void testInnerAnalyzable() {
        GrelExpr ev = new FieldAccessorExpr(constant, "foo");
        assertEquals(ev.getColumnDependencies(baseColumn), set());
        ev = new FieldAccessorExpr(currentColumn, "foo");
        assertEquals(ev.getColumnDependencies(baseColumn), set(baseColumn));
        ev = new FieldAccessorExpr(twoColumns, "foo");
        assertEquals(ev.getColumnDependencies(baseColumn), set("a", "b"));
    }

    @Test
    public void testUnanalyzable() {
        when(unanalyzable.toString()).thenReturn("bar");
        GrelExpr ev = new FieldAccessorExpr(unanalyzable, "foo");
        assertNull(ev.getColumnDependencies(baseColumn));
        assertFalse(ev.isLocal());
    }

    @Test
    public void testCells() {
        when(unanalyzable.toString()).thenReturn("cells");
        GrelExpr ev = new FieldAccessorExpr(unanalyzable, "foo");
        assertEquals(ev.getColumnDependencies(baseColumn), set("foo"));
    }
}
