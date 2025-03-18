
package com.google.refine.grel.ast;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import java.util.Optional;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.expr.Evaluable;
import com.google.refine.grel.Function;

public class FunctionCallExprTest extends ExprTestBase {

    protected Function function;

    @BeforeTest
    public void setUpFunction() {
        function = mock(Function.class);
    }

    @Test
    public void testInvalidConstruct() {
        assertThrows(IllegalArgumentException.class, () -> new FunctionCallExpr(new Evaluable[] {}, function, "fun", true));
    }

    @Test
    public void testSource() {
        Evaluable ev = new FunctionCallExpr(new Evaluable[] { constant, currentColumn, twoColumns }, function, "fun", false);
        when(constant.toString()).thenReturn("a");
        when(currentColumn.toString()).thenReturn("b");
        when(twoColumns.toString()).thenReturn("c");

        assertEquals(ev.getSource(), "fun(a, b, c)");
    }

    @Test
    public void testSourceInFluentStyle() {
        Evaluable ev = new FunctionCallExpr(new Evaluable[] { constant, currentColumn, twoColumns }, function, "fun", true);
        when(constant.toString()).thenReturn("a");
        when(currentColumn.toString()).thenReturn("b");
        when(twoColumns.toString()).thenReturn("c");

        assertEquals(ev.getSource(), "a.fun(b, c)");
    }

    @Test
    public void testUnion() {
        Evaluable ev = new FunctionCallExpr(new Evaluable[] { constant, currentColumn, twoColumns }, function, "fun", false);
        assertEquals(ev.getColumnDependencies(baseColumn), set("baseColumn", "a", "b"));
        assertEquals(ev.renameColumnDependencies(sampleRename),
                new FunctionCallExpr(new Evaluable[] { constant, currentColumnRenamed, twoColumnsRenamed }, function, "fun",
                        false));
    }

    @Test
    public void testUnanalyzable() {
        Evaluable ev = new FunctionCallExpr(new Evaluable[] { currentColumn, unanalyzable }, function, "fun", false);
        assertEquals(ev.getColumnDependencies(baseColumn), Optional.empty());
        assertEquals(ev.renameColumnDependencies(sampleRename),
                new FunctionCallExpr(new Evaluable[] { currentColumnRenamed, unanalyzable }, function, "fun", false));
    }
}
