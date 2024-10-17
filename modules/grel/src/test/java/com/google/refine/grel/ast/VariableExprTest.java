
package com.google.refine.grel.ast;

import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.Test;

public class VariableExprTest extends ExprTestBase {

    @Test
    public void testBaseColumn() {
        GrelExpr ev = new VariableExpr("value");
        assertEquals(ev.getColumnDependencies(baseColumn), set("baseColumn"));
        ev = new VariableExpr("cell");
        assertEquals(ev.getColumnDependencies(baseColumn), set("baseColumn"));
        ev = new VariableExpr("recon");
        assertEquals(ev.getColumnDependencies(baseColumn), set("baseColumn"));
    }

    @Test
    public void testUnanalyzable() {
        GrelExpr ev = new VariableExpr("cells");
        assertEquals(ev.getColumnDependencies(baseColumn), Optional.empty());
        ev = new VariableExpr("row");
        assertEquals(ev.getColumnDependencies(baseColumn), Optional.empty());
        ev = new VariableExpr("record");
        assertEquals(ev.getColumnDependencies(baseColumn), Optional.empty());
    }

    @Test
    public void testSingleton() {
        GrelExpr ev = new VariableExpr("foo");
        assertEquals(ev.getColumnDependencies(baseColumn), set());
    }
}
