
package com.google.refine.expr.functions.arrays;

import static org.testng.AssertJUnit.assertEquals;

import java.util.List;

import com.google.common.collect.Lists;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.GrelTestBase;

public class ZipTests extends GrelTestBase {

    @Test
    public void zipArray() throws ParsingException {
        String[] test = { "zip([1,2,3], ['A','B','C'])", "[[1, A], [2, B], [3, C]]" };
        parseEval(bindings, test);

        String[] test1 = { "zip([1,2,3], ['A','B','C'], ['X','Y','Z'])", "[[1, A, X], [2, B, Y], [3, C, Z]]" };
        parseEval(bindings, test1);

        String[] test2 = { "zip([1,2], ['A','B','C'], ['X','Y','Z'])", "[[1, A, X], [2, B, Y]]" };
        parseEval(bindings, test2);

        String[] test3 = { "zip([1,2,3], ['A','B'], ['X','Y','Z'])", "[[1, A, X], [2, B, Y]]" };
        parseEval(bindings, test3);

        String[] test4 = { "zip([1,2,3], ['A','B','C'], ['X','Y'])", "[[1, A, X], [2, B, Y]]" };
        parseEval(bindings, test4);

        String[] test5 = { "zip([1,2,3], ['A','B','C'], [])", "[]" };
        parseEval(bindings, test5);

        String[] test6 = {
                "zip( [\"Bob has a cat & dog\", \"Cat's name is Dotty\", \"Dog's name is Dots\"], [11, 22, 33, 44, 55], ['Doe', 'Foe', 456, 789])",
                "[[Bob has a cat & dog, 11, Doe], [Cat's name is Dotty, 22, Foe], [Dog's name is Dots, 33, 456]]" };
        parseEval(bindings, test6);

        String[] test7 = { "zip([1,2,3], ['A',null,'C'], ['X','Y'])", "[[1, A, X], [2, null, Y]]" };
        parseEval(bindings, test7);

        // Tests for JSON array
        String[] test8 = {
                "zip([\"Bob has a cat & dog\", \"Cat's name is Dotty\", \"Dog's name is Dots\"], [11, 22, 33, 44, 55], '[\"Doe\", \"Foe\", 456, 789]'.parseJson() )",
                "[[Bob has a cat & dog, 11, \"Doe\"], [Cat's name is Dotty, 22, \"Foe\"], [Dog's name is Dots, 33, 456]]" };
        parseEval(bindings, test8);

        // TODO: Add tests for List<Object> returned from ExpressionUtils.toObjectList()
    }

    @Test
    public void testZipwithIntArrays() {
        List arg1 = List.of(1, 2, 3);
        List arg2 = List.of(7.89, 8.90, 9.01);

        List<List> expected = List.of(
                List.of(1, 7.89),
                List.of(2, 8.90),
                List.of(3, 9.01));

        assertEquals(invoke("zip", arg1, arg2), expected);
    }

    @Test
    public void testZipwithStrArrays() {
        List arg1 = List.of("Ben", "Den", "Hen");
        List arg2 = List.of("A", "B", "C");

        List<List> expected = List.of(
                List.of("Ben", "A"),
                List.of("Den", "B"),
                List.of("Hen", "C"));

        assertEquals(invoke("zip", arg1, arg2), expected);
    }

    @Test
    public void testZipwithAllTypeArrays() {
        List arg1 = Lists.newArrayList(1, null, 3.142);
        List arg2 = Lists.newArrayList("A", "B", "C");
        List arg3 = Lists.newArrayList("Ben", null, "Hen");

        List<List> expected = List.of(
                Lists.newArrayList(1, "A", "Ben"),
                Lists.newArrayList(null, "B", null),
                Lists.newArrayList(3.142, "C", "Hen"));

        assertEquals(invoke("zip", arg1, arg2, arg3), expected);
    }

    @Test
    public void testZipForParams() {
        List arg1 = List.of(1, 2, 3);
        List arg2 = List.of(7.89, 8.90, 9.01);

        List<List> expected = List.of(
                List.of(1, 7.89),
                List.of(2, 8.90),
                List.of(3, 9.01));

        assertEquals(((EvalError) (invoke("zip", arg1, arg2, null))).message, "zip expects 2 or more arrays as arguments");

        assertEquals(((EvalError) (invoke("zip", arg1, arg2, "test", null))).message, "zip expects 2 or more arrays as arguments");

    }
}
