
package com.google.refine.expr.functions.strings;

import java.util.Properties;

import org.junit.Assert;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.ParsingException;

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

    @Test
    public void testReplaceEachWithReplaceStrArray() throws ParsingException {
        String test[] = { "\"The cow jumps over the moon and moos\".replaceEach([\"th\", \"moo\"], [\"ex\", \"mee\"])",
                "The cow jumps over exe meen and mees" };
        bindings = new Properties();
        bindings.put("v", "");
        parseEval(bindings, test);
    }

    @Test
    public void testReplaceEachWithReplaceStr() throws ParsingException {
        String test[] = { "\"abcdefghijklmnopqrstuvwxyz\".replaceEach([\"a\",\"e\",\"i\",\"o\",\"u\"], \"A\")",
                "AbcdAfghAjklmnApqrstAvwxyz" };
        bindings = new Properties();
        bindings.put("v", "");
        parseEval(bindings, test);
    }

    @Test
    public void testReplaceEachWithReplaceArrShorterThanSearchArr() throws ParsingException {
        String test[] = { "\"abcdefghijklmnopqrstuvwxyz\".replaceEach([\"a\",\"e\",\"i\",\"o\",\"u\"], [\"A\", \"E\", \"I\"])",
                "IbcdIfghIjklmnIpqrstIvwxyz" };
        bindings = new Properties();
        bindings.put("v", "");
        parseEval(bindings, test);
    }

    @Test
    public void testReplaceEachWithInteger() throws ParsingException {
        String test[] = { "\"123456789\".replaceEach([1,2,3], [6,7,8])",
                "678456789" };
        bindings = new Properties();
        bindings.put("v", "");
        parseEval(bindings, test);
    }

    @Test
    public void testReplaceEachWithFloat() throws ParsingException {
        String test[] = { "\"1.52.53.5456789\".replaceEach([1.5,2.5,3.5], [6.0,7.0,8.0])",
                "6.07.08.0456789" };
        bindings = new Properties();
        bindings.put("v", "");
        parseEval(bindings, test);
    }

    @Test
    public void testReplaceEachWithNull() throws ParsingException {
        String test[] = { "\"abcdefg\".replaceEach([\"a\",\"b\",\"c\"], [\"A\",\"\",null])",
                "Adefg" };
        bindings = new Properties();
        bindings.put("v", "");
        parseEval(bindings, test);
    }
}
