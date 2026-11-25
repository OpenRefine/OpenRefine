
package com.google.refine.grel.ast;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.grel.Function;
import com.google.refine.util.ParsingUtilities;

public class SpreadExprTest extends ExprTestBase {

    @Test
    public void testGetSource() {
        when(constant.toString()).thenReturn("'foo'");
        assertEquals(new SpreadExpr(constant).toString(), "'foo'...");
    }

    @Test
    public void testGetColumnDependencies() {
        assertEquals(new SpreadExpr(constant).getColumnDependencies(baseColumn), Optional.of(Set.of()));
        assertEquals(new SpreadExpr(currentColumn).getColumnDependencies(baseColumn), Optional.of(Set.of("baseColumn")));
        assertEquals(new SpreadExpr(unanalyzable).getColumnDependencies(baseColumn), Optional.empty());
    }

    @Test
    public void testRenameColumnDependencies() {
        GrelExpr spreadConstant = new SpreadExpr(constant);
        assertEquals(spreadConstant.renameColumnDependencies(sampleRename), spreadConstant);
        GrelExpr spreadVariable = new SpreadExpr(currentColumn);
        assertEquals(spreadVariable.renameColumnDependencies(sampleRename), new SpreadExpr(currentColumnRenamed));
    }

    @Test
    public void testToElements() throws Exception {
        Object[] array = new Object[] { "a", 1L };
        assertEquals(SpreadExpr.toElements(array), array);
        assertEquals(SpreadExpr.toElements(new String[] { "a", "b" }), new Object[] { "a", "b" });
        assertEquals(SpreadExpr.toElements(new int[] { 1, 2 }), new Object[] { 1, 2 });
        assertEquals(SpreadExpr.toElements(List.of("a", true)), new Object[] { "a", true });

        ArrayNode json = (ArrayNode) ParsingUtilities.mapper.readTree("[\"a\", 1, 2.5, true, null, [3]]");
        Object[] jsonElements = SpreadExpr.toElements(json);
        assertEquals(jsonElements.length, 6);
        assertEquals(jsonElements[0], "a");
        assertEquals(jsonElements[1], 1);
        assertEquals(jsonElements[2], 2.5);
        assertEquals(jsonElements[3], true);
        assertNull(jsonElements[4]);
        assertEquals(jsonElements[5], json.get(5));

        assertEquals(SpreadExpr.toElements(new Object[] {}), new Object[] {});
    }

    @Test
    public void testToElementsRejectsNonArrays() throws Exception {
        assertNull(SpreadExpr.toElements(null));
        assertNull(SpreadExpr.toElements("abc"));
        assertNull(SpreadExpr.toElements(12L));
        assertNull(SpreadExpr.toElements(true));
        assertNull(SpreadExpr.toElements(ParsingUtilities.mapper.readTree("{\"a\":1}")));
    }

    @Test
    public void testFunctionCallSplicesElements() {
        Function function = mock(Function.class);
        Properties bindings = new Properties();
        when(constant.evaluate(bindings)).thenReturn("first");
        when(currentColumn.evaluate(bindings)).thenReturn(new Object[] { "x", "y" });
        when(twoColumns.evaluate(bindings)).thenReturn(List.of("z"));
        when(function.call(any(), any())).thenReturn("result");

        Evaluable ev = new FunctionCallExpr(
                new Evaluable[] { constant, new SpreadExpr(currentColumn), new SpreadExpr(twoColumns) },
                function, "fun", false);

        assertEquals(ev.evaluate(bindings), "result");
        verify(function).call(bindings, new Object[] { "first", "x", "y", "z" });
    }

    @Test
    public void testFunctionCallRejectsNonArray() {
        Function function = mock(Function.class);
        Properties bindings = new Properties();
        when(constant.evaluate(bindings)).thenReturn("abc");

        Evaluable ev = new FunctionCallExpr(new Evaluable[] { new SpreadExpr(constant) }, function, "fun", false);

        assertTrue(ev.evaluate(bindings) instanceof EvalError);
        verifyNoInteractions(function);
    }

    @Test
    public void testFunctionCallBubblesUpError() {
        Function function = mock(Function.class);
        Properties bindings = new Properties();
        EvalError error = new EvalError("boom");
        when(constant.evaluate(bindings)).thenReturn(error);

        Evaluable ev = new FunctionCallExpr(new Evaluable[] { new SpreadExpr(constant) }, function, "fun", false);

        assertEquals(ev.evaluate(bindings), error);
        verifyNoInteractions(function);
    }
}
