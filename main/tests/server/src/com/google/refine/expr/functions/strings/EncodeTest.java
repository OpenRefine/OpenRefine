
package com.google.refine.expr.functions.strings;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import org.testng.Assert;
import org.testng.annotations.Test;

public class EncodeTest extends RefineTest {

    @Test
    public void testEncode() {
        String[][] inputs = {
                // base16
                { "abc", "base16", "616263" },
                { "a\n\r\nb", "base16", "610A0D0A62" },

                // base32
                { "abc", "base32", "MFRGG===" },
                { "a\n\r\nb", "base32", "MEFA2CTC" },

                // base32hex
                { "abc", "base32hex", "C5H66===" },
                { "a\n\r\nb", "base32hex", "C450Q2J2" },

                // base64
                { "abc", "base64", "YWJj" },
                { "a\n\r\nb", "base64", "YQoNCmI=" },

                // base64url
                { "abc", "base64url", "YWJj" },
                { "a\n\r\nb", "base64url", "YQoNCmI=" },
        };

        for (String[] input : inputs) {
            String string = input[0];
            String encoding = input[1];
            String expected = input[2];
            Assert.assertEquals(invoke("encode", string, encoding), expected);
        }

    }

    @Test
    public void testEncodeInvalidParams() {
        Assert.assertTrue(invoke("encode", "abc", "base16", "base32") instanceof EvalError);
        Assert.assertTrue(invoke("encode", "base64") instanceof EvalError);
        Assert.assertTrue(invoke("encode", "abc") instanceof EvalError);
        Assert.assertTrue(invoke("encode", 2, "base16") instanceof EvalError);
        Assert.assertTrue(invoke("encode", "abc", "encoding") instanceof EvalError);
    }
}
