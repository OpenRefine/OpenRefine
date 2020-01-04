package org.openrefine.grel.ast;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

import org.openrefine.expr.Evaluable;

public class OperatorCallExprTest extends ExprTestBase {
    protected String operator = "+";
    
    @Test
    public void testUnion() {
        Evaluable ev = new OperatorCallExpr(new Evaluable[] {constant,currentColumn,twoColumns}, operator);
        assertEquals(ev.getColumnDependencies(baseColumn), set(baseColumn, "a", "b"));
    }
    
    @Test
    public void testUnanalyzable() {
        Evaluable ev = new OperatorCallExpr(new Evaluable []{currentColumn,unanalyzable}, operator);
        assertNull(ev.getColumnDependencies(baseColumn));
    }
}
