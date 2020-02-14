
package org.openrefine.grel.ast;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

public class VariableExprTest extends ExprTestBase {

    @Test
    public void testBaseColumn() {
        GrelExpr ev = new VariableExpr("value");
        assertEquals(ev.getColumnDependencies(baseColumn), set(baseColumn));
        ev = new VariableExpr("cell");
        assertEquals(ev.getColumnDependencies(baseColumn), set(baseColumn));
        ev = new VariableExpr("recon");
        assertEquals(ev.getColumnDependencies(baseColumn), set(baseColumn));
    }

    @Test
    public void testUnanalyzable() {
        GrelExpr ev = new VariableExpr("cells");
        assertNull(ev.getColumnDependencies(baseColumn));
        ev = new VariableExpr("row");
        assertNull(ev.getColumnDependencies(baseColumn));
        ev = new VariableExpr("record");
        assertNull(ev.getColumnDependencies(baseColumn));
    }

    @Test
    public void testSingleton() {
        GrelExpr ev = new VariableExpr("foo");
        assertEquals(ev.getColumnDependencies(baseColumn), set());
    }
}
