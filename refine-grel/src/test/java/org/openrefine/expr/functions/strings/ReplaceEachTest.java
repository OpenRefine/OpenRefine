
package org.openrefine.expr.functions.strings;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.expr.EvalError;
import org.openrefine.grel.FunctionTestBase;

public class ReplaceEachTest extends FunctionTestBase {

    @Test
    public void replaceEachValidParams() {

        Assert.assertTrue(
                invoke("replaceEach", "abcdefghijklmnopqrstuvwxyz", new String[] { "a", "e", "i", "o", "u" }, "A") instanceof String);
        Assert.assertEquals(invoke("replaceEach", "abcdefghijklmnopqrstuvwxyz", new String[] { "a", "e", "i", "o", "u" }, "A"),
                "AbcdAfghAjklmnApqrstAvwxyz");
        Assert.assertEquals(
                invoke("replaceEach", "abcdefghijklmnopqrstuvwxyz", new String[] { "a", "e", "i", "o", "u" }, new String[] { "A" }),
                "AbcdAfghAjklmnApqrstAvwxyz");
        Assert.assertEquals(invoke("replaceEach", "abcdefghijklmnopqrstuvwxyz", new String[] { "a", "e", "i", "o", "u" },
                new String[] { "A", "E", "I", "O", "U" }), "AbcdEfghIjklmnOpqrstUvwxyz");

    }

    @Test
    public void replaceEachInvalidParams() {
        Assert.assertTrue(invoke("replaceEach") instanceof EvalError);
        Assert.assertTrue(invoke("replaceEach", "abcdefghijklmnopqrstuvwxyz") instanceof EvalError);
        Assert.assertTrue(
                invoke("replaceEach", "abcdefghijklmnopqrstuvwxyz", new String[] { "a", "e", "i", "o", "u" }) instanceof EvalError);
        Assert.assertTrue(invoke("replaceEach", "abcdefghijklmnopqrstuvwxyz", new String[] { "a", "e", "i", "o", "u" }, "A",
                "B") instanceof EvalError);
        Assert.assertTrue(invoke("replaceEach", new String[] { "a", "e", "i", "o", "u" }, new String[] { "A", "B" },
                "abcdefghijklmnopqrstuvwxyz") instanceof EvalError);
    }
}
