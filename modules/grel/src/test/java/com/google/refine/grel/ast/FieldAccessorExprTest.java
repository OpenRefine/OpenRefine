
package com.google.refine.grel.ast;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.util.Map;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.refine.expr.Evaluable;

public class FieldAccessorExprTest extends ExprTestBase {

    @Test
    public void testInnerAnalyzable() {
        Evaluable ev = new FieldAccessorExpr(constant, "foo");
        assertEquals(ev.getColumnDependencies(baseColumn), set());
        assertEquals(ev.renameColumnDependencies(sampleRename), Optional.of(ev));

        ev = new FieldAccessorExpr(currentColumn, "foo");
        assertEquals(ev.getColumnDependencies(baseColumn), set("baseColumn"));
        assertEquals(ev.renameColumnDependencies(sampleRename), Optional.of(new FieldAccessorExpr(currentColumnRenamed, "foo")));

        ev = new FieldAccessorExpr(twoColumns, "foo");
        assertEquals(ev.getColumnDependencies(baseColumn), set("a", "b"));
        assertEquals(ev.renameColumnDependencies(sampleRename), Optional.of(new FieldAccessorExpr(twoColumnsRenamed, "foo")));
    }

    @Test
    public void testUnanalyzable() {
        when(unanalyzable.toString()).thenReturn("bar");
        Evaluable ev = new FieldAccessorExpr(unanalyzable, "foo");
        assertEquals(ev.getColumnDependencies(baseColumn), Optional.empty());
        assertEquals(ev.renameColumnDependencies(sampleRename), Optional.empty());
    }

    @Test
    public void testCells() {
        when(unanalyzable.toString()).thenReturn("cells");
        Evaluable ev = new FieldAccessorExpr(unanalyzable, "foo");
        assertEquals(ev.getColumnDependencies(baseColumn), set("foo"));
        assertEquals(ev.renameColumnDependencies(Map.of("foo", "bar")), Optional.of(new FieldAccessorExpr(unanalyzable, "bar")));
    }
}
