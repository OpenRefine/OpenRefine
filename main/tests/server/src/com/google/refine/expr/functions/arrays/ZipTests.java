
package com.google.refine.expr.functions.arrays;

import org.testng.annotations.Test;

import com.google.refine.expr.ParsingException;
import com.google.refine.grel.GrelTestBase;

public class ZipTests extends GrelTestBase {

    @Test
    public void zipArray() throws ParsingException {
        String[] test = { "[ [1,2,3], ['A','B','C'] ].zip()", "[[1, A], [2, B], [3, C]]" };
        parseEval(bindings, test);

        String[] test1 = { "[[1,2,3], ['A','B','C'], ['X','Y','Z']].zip()", "[[1, A, X], [2, B, Y], [3, C, Z]]" };
        parseEval(bindings, test1);

        String[] test2 = { "[[1,2], ['A','B','C'], ['X','Y','Z']].zip()", "[[1, A, X], [2, B, Y]]" };
        parseEval(bindings, test2);

        String[] test3 = { "[[1,2,3], ['A','B'], ['X','Y','Z']].zip()", "[[1, A, X], [2, B, Y]]" };
        parseEval(bindings, test3);

        String[] test4 = { "[[1,2,3], ['A','B','C'], ['X','Y']].zip()", "[[1, A, X], [2, B, Y]]" };
        parseEval(bindings, test4);

        String[] test5 = { "[[1,2,3], ['A','B','C'], []].zip()", "[]" };
        parseEval(bindings, test5);

        String[] test6 = {
                "[ [\"Bob has a cat & dog\", \"Cat's name is Dotty\", \"Dog's name is Dots\"], [11, 22, 33, 44, 55], ['Doe', 'Foe', 456, 789]].zip()",
                "[[Bob has a cat & dog, 11, Doe], [Cat's name is Dotty, 22, Foe], [Dog's name is Dots, 33, 456]]" };
        parseEval(bindings, test6);

        String[] test7 = { "[[1,2,3], ['A',null,'C'], ['X','Y']].zip()", "[[1, A, X], [2, null, Y]]" };
        parseEval(bindings, test7);

        // Tests for JSON array
        String[] test8 = {
                "[ [\"Bob has a cat & dog\", \"Cat's name is Dotty\", \"Dog's name is Dots\"], [11, 22, 33, 44, 55], '[\"Doe\", \"Foe\", 456, 789]'.parseJson() ].zip()",
                "[[Bob has a cat & dog, 11, \"Doe\"], [Cat's name is Dotty, 22, \"Foe\"], [Dog's name is Dots, 33, 456]]" };
        parseEval(bindings, test8);

        // TODO: Add tests for List<Object> returned from ExpressionUtils.toObjectList()
    }

}
