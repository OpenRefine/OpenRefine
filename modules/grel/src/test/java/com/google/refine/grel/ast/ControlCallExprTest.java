
package com.google.refine.grel.ast;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.expr.Evaluable;
import com.google.refine.grel.Control;

public class ControlCallExprTest extends ExprTestBase {

    Control control;

    @BeforeMethod
    public void setUpControl() {
        control = mock(Control.class);
    }

    @Test
    public void testSource() {
        Evaluable e = new ControlCallExpr(new Evaluable[] {}, control, "myControl");
        assertEquals(e.getSource(), "myControl()");
    }

    @Test
    public void testConstant() {
        Evaluable c = new ControlCallExpr(new Evaluable[] { constant }, control, "myControl");
        assertEquals(c.getColumnDependencies(baseColumn), set());
        assertEquals(c.renameColumnDependencies(sampleRename), c);
    }

    @Test
    public void testUnion() {
        Evaluable c = new ControlCallExpr(new Evaluable[] { twoColumns, currentColumn }, control, "myControl");
        assertEquals(c.getColumnDependencies(baseColumn), set("a", "b", "baseColumn"));
        assertEquals(c.renameColumnDependencies(sampleRename),
                new ControlCallExpr(new Evaluable[] { twoColumnsRenamed, currentColumnRenamed }, control, "myControl"));
    }

    @Test
    public void testUnanalyzable() {
        Evaluable c = new ControlCallExpr(new Evaluable[] { twoColumns, unanalyzable }, control, "myControl");
        assertEquals(c.getColumnDependencies(baseColumn), Optional.empty());
        assertEquals(c.renameColumnDependencies(sampleRename),
                new ControlCallExpr(new Evaluable[] { twoColumnsRenamed, unanalyzable }, control, "myControl"));
    }
}
