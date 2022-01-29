
package org.openrefine.grel.ast;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

public class VariableExprTest extends ExprTestBase {

    @Test
    public void testBaseColumn() {
        GrelExpr ev = new VariableExpr("value");
        assertEquals(ev.getColumnDependencies(baseColumn), set(baseColumn));
        assertTrue(ev.isLocal());
        ev = new VariableExpr("cell");
        assertEquals(ev.getColumnDependencies(baseColumn), set(baseColumn));
        assertTrue(ev.isLocal());
        ev = new VariableExpr("recon");
        assertEquals(ev.getColumnDependencies(baseColumn), set(baseColumn));
        assertTrue(ev.isLocal());
    }

    @Test
    public void testUnanalyzable() {
        GrelExpr ev = new VariableExpr("cells");
        assertNull(ev.getColumnDependencies(baseColumn));
        assertTrue(ev.isLocal());
        ev = new VariableExpr("row");
        assertNull(ev.getColumnDependencies(baseColumn));
        assertTrue(ev.isLocal());
        ev = new VariableExpr("record");
        assertNull(ev.getColumnDependencies(baseColumn));
        assertTrue(ev.isLocal());
    }

    @Test
    public void testSingleton() {
        GrelExpr ev = new VariableExpr("foo");
        assertEquals(ev.getColumnDependencies(baseColumn), set());
        assertTrue(ev.isLocal());
    }
}
