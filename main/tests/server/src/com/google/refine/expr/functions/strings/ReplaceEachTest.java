
package com.google.refine.expr.functions.strings;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import org.junit.Assert;
import org.testng.annotations.Test;

public class ReplaceEachTest extends RefineTest {

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
