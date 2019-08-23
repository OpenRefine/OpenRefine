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

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.Contains;
import com.google.refine.util.TestUtils;

import java.util.Properties;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineServlet;
import com.google.refine.RefineServletStub;
import com.google.refine.RefineTest;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

/**
 * Test cases for find function.
 */
public class ContainsTests extends RefineTest {
	
    static Properties bindings;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    RefineServlet servlet;

    @BeforeMethod
    public void SetUp() {
        bindings = new Properties();

        servlet = new RefineServletStub();
    }

    @AfterMethod
    public void TearDown() {
    }


    @Test
    public void testContainsFunction() {
        String value = "rose is a rose";
        Assert.assertEquals(invoke("contains", value, "rose"),true);

        // Test if it does not interpret regex passed in String as Pattern
        Assert.assertEquals(invoke("contains", value, "$"),false);
        Assert.assertEquals(invoke("contains", value, "r.se"),false);
        Assert.assertEquals(invoke("contains", value, "\\s+"),false);
        // Input regex pattern in UI with : "/ /" , is intepreted as Pattern
        Assert.assertEquals(invoke("contains", value, Pattern.compile("$")),true);
        Assert.assertEquals(invoke("contains", value, Pattern.compile("\\s+")),true);
    }

    /**
     * Lookup a control function by name and invoke it with a variable number of args
     */
    private static Object invoke(String name,Object... args) {
        // registry uses static initializer, so no need to set it up
        Function function = ControlFunctionRegistry.getFunction(name);
        if (function == null) {
            throw new IllegalArgumentException("Unknown function "+name);
        }
        if (args == null) {
            return function.call(bindings,new Object[0]);
        } else {
            return function.call(bindings,args);
        }
    }
    
    @Test
    public void serializeContains() {
        String json = "{\"description\":\"Returns whether s contains frag\",\"params\":\"string s, string frag\",\"returns\":\"boolean\"}";
        TestUtils.isSerializedTo(new Contains(), json);
    }
}

