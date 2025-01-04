
package com.google.refine.grel.ast;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.refine.expr.Evaluable;

public class ArrayExprTest extends ExprTestBase {

    @Test
    public void testSource() {
        Evaluable ev = new ArrayExpr(new Evaluable[] { constant, currentColumn, twoColumns });
        when(constant.toString()).thenReturn("a");
        when(currentColumn.toString()).thenReturn("b");
        when(twoColumns.toString()).thenReturn("c");

        assertEquals(ev.getSource(), "[a, b, c]");
    }

    @Test
    public void testColumnDependencies() {
        Evaluable ev = new ArrayExpr(new Evaluable[] { constant, currentColumn, twoColumns });
        assertEquals(ev.getColumnDependencies(baseColumn), set("baseColumn", "a", "b"));
        assertEquals(ev.renameColumnDependencies(sampleRename),
                new ArrayExpr(new Evaluable[] { constant, currentColumnRenamed, twoColumnsRenamed }));
    }
}
