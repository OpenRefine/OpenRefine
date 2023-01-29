
package org.openrefine.grel.ast;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

public class OperatorCallExprTest extends ExprTestBase {

    protected String operator = "+";

    @Test
    public void testUnion() {
        GrelExpr ev = new OperatorCallExpr(new GrelExpr[] { constant, currentColumn, twoColumns }, operator);
        assertEquals(ev.getColumnDependencies(baseColumn), set(baseColumn, "a", "b"));
    }

    @Test
    public void testUnanalyzable() {
        GrelExpr ev = new OperatorCallExpr(new GrelExpr[] { currentColumn, unanalyzable }, operator);
        assertNull(ev.getColumnDependencies(baseColumn));
    }
}
