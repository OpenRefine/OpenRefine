/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.expr.functions.strings;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.util.TestUtils;

public class TrimTests extends RefineTest {

    private static String NBSP = "\u00A0";
    private static String ENQUAD = "\u2000";
    private static String EMQUAD = "\u2001";
    private static String ENSPC = "\u2002";
    private static String EMSPC = "\u2003";
    private static String N3PMSPC = "\u2004";
    private static String N4PMSPC = "\u2005";
    private static String N6PMSPC = "\u2006";
    private static String FIGSP = "\u2007";
    private static String PUNCSPC = "\u2008";
    private static String THINSPC = "\u2009";
    private static String HAIRSPC = "\u200A";
    private static String NNBSP = "\u202F";
    private static String MDMMATHSPC = "\u205F";
//    private static String ZWNBSP = "\uFEFF";
//    private static String WDJOIN = "\u2060";
    private static String IDEOSPC = "\u3000";

    private static String WHITESPACE = NBSP + ENQUAD + ENSPC + EMQUAD + EMSPC + N3PMSPC + N4PMSPC + N6PMSPC + FIGSP + PUNCSPC
            + THINSPC + HAIRSPC + NNBSP + MDMMATHSPC
//            +ZWNBSP
//            +WDJOIN
            + IDEOSPC;

    private static String[][] testStrings = {
            { " foo ", "foo" },
            { "\tfoo\t", "foo" },
            { "\t \t foo \t \t", "foo" },
//        {WHITESPACE+"foo"+WHITESPACE,"foo"},  
            { "", "" },
    };

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Test
    public void testInvalidParams() {
        Assert.assertTrue(invoke("trim") instanceof EvalError);
        Assert.assertTrue(invoke("trim", "one", "two", "three") instanceof EvalError);
        Assert.assertTrue(invoke("trim", Long.getLong("1")) instanceof EvalError);
    }

    @Test
    public void testTrim() {
        for (String[] ss : testStrings) {
            Assert.assertEquals(ss.length, 2, "Invalid test"); // Not a valid test
            Assert.assertEquals((String) (invoke("trim", ss[0])), ss[1], "Trim for string: " + ss + " failed");
        }

        for (int i = 0; i < WHITESPACE.length(); i++) {
            String c = WHITESPACE.substring(i, i + 1);
            Assert.assertEquals((String) (invoke("trim", c + "foo" + c)), "foo",
                    "Trim for whitespace char: '" + c + "' at index " + i + " failed");
        }

    }
}
