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

import java.util.Properties;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineServlet;
import com.google.refine.RefineTest;
import com.google.refine.util.TestUtils;

/**
 * Test cases for find function.
 */
public class FindTests extends RefineTest {

    static Properties bindings;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    RefineServlet servlet;

    @Test
    public void findFunctionFindAllTest() throws Exception {
        String[] matches = (String[]) invoke("find", "This is a test string for testing find.", "test");
        Assert.assertEquals(matches[0], "test");
        Assert.assertEquals(matches[1], "test");
    }

    @Test
    public void findFunctionFindLiteralTest() throws Exception {
        String[] matches = (String[]) invoke("find", "This is a test string for testing find.", ".test");
        Assert.assertEquals(matches.length, 0);
    }

    @Test
    public void findFunctionFindRegexTest() throws Exception {
        String[] matches = (String[]) invoke("find", "hello 123456 goodbye.", Pattern.compile("\\d{6}|hello"));
        Assert.assertEquals(matches[0], "hello");
        Assert.assertEquals(matches[1], "123456");
    }

}
