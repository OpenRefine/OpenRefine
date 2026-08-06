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

import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineServlet;
import com.google.refine.grel.GrelTestBase;

/**
 * Test cases for the match function, including Unicode-aware regex (#1768).
 */
public class MatchTests extends GrelTestBase {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    RefineServlet servlet;

    @Test
    public void matchFunctionLiteralStringTest() throws Exception {
        // Plain ASCII still matches end-to-end.
        Assert.assertEquals(((String[]) invoke("match", "hello", "h(e)llo"))[0], "e");
    }

    @Test
    public void matchFunctionUnicodeStringRegexTest() throws Exception {
        // String-form regex: \w must cover Unicode letters (#1768). "café" should match \\w+ as a whole word.
        String[] groups = (String[]) invoke("match", "café", "\\w+");
        Assert.assertNotNull(groups, "expected a match for 'café' against \\w+ with Unicode character class");
        Assert.assertEquals(groups.length, 0, "no capture groups expected for \\w+");
    }

    @Test
    public void matchFunctionUnicodeStringRegexWithGroupTest() throws Exception {
        // Capture groups work and Unicode word characters are matched by \\w.
        String[] groups = (String[]) invoke("match", "Ångström", "(\\w+)");
        Assert.assertNotNull(groups);
        Assert.assertEquals(groups[0], "Ångström");
    }

    @Test
    public void matchFunctionUnicodePatternRegexTest() throws Exception {
        // Pattern-form regex compiled with the flag should also see Unicode word characters.
        Pattern p = Pattern.compile("\\w+", Pattern.UNICODE_CHARACTER_CLASS);
        String[] groups = (String[]) invoke("match", "café", p);
        Assert.assertNotNull(groups);
    }

    @Test
    public void matchFunctionUnicodeDigitsTest() throws Exception {
        // Arabic-Indic digits should be matched by \\d under UNICODE_CHARACTER_CLASS.
        String[] groups = (String[]) invoke("match", "١٢٣", "\\d+");
        Assert.assertNotNull(groups, "expected \\d to match Arabic-Indic digits under Unicode flag");
    }

    @Test
    public void matchFunctionNoMatchReturnsNullTest() throws Exception {
        Assert.assertNull(invoke("match", "abc", "\\d+"));
    }
}
