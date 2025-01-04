
package com.google.refine.grel.ast;

import static org.testng.Assert.assertEquals;

import java.util.Map;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.refine.expr.Evaluable;

public class VariableExprTest extends ExprTestBase {

    @Test
    public void testBaseColumn() {
        Evaluable ev = new VariableExpr("value");
        assertEquals(ev.getColumnDependencies(baseColumn), set("baseColumn"));
        assertEquals(ev.renameColumnDependencies(Map.of("someColumn", "newColumn")), ev);

        ev = new VariableExpr("cell");
        assertEquals(ev.getColumnDependencies(baseColumn), set("baseColumn"));
        assertEquals(ev.renameColumnDependencies(Map.of("someColumn", "newColumn")), ev);

        ev = new VariableExpr("recon");
        assertEquals(ev.getColumnDependencies(baseColumn), set("baseColumn"));
        assertEquals(ev.renameColumnDependencies(Map.of("someColumn", "newColumn")), ev);
    }

    @Test
    public void testUnanalyzable() {
        Evaluable ev = new VariableExpr("cells");
        assertEquals(ev.getColumnDependencies(baseColumn), Optional.empty());
        assertEquals(ev.renameColumnDependencies(Map.of("someColumn", "newColumn")), ev);

        ev = new VariableExpr("row");
        assertEquals(ev.getColumnDependencies(baseColumn), Optional.empty());
        assertEquals(ev.renameColumnDependencies(Map.of("someColumn", "newColumn")), ev);

        ev = new VariableExpr("record");
        assertEquals(ev.getColumnDependencies(baseColumn), Optional.empty());
        assertEquals(ev.renameColumnDependencies(Map.of("someColumn", "newColumn")), ev);
    }

    @Test
    public void testSingleton() {
        Evaluable ev = new VariableExpr("foo");
        assertEquals(ev.getColumnDependencies(baseColumn), set());
        assertEquals(ev.renameColumnDependencies(Map.of("someColumn", "newColumn")), ev);
    }
}
