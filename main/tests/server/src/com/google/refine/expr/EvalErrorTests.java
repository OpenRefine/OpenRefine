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

package com.google.refine.expr;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.RefineTest;
import com.google.refine.util.TestUtils;

public class EvalErrorTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Test
    public void serializeEvalError() {
        EvalError e = new EvalError("This is a critical error");
        TestUtils.isSerializedTo(e, "{\"type\":\"error\",\"message\":\"This is a critical error\"}");
    }

    @Test
    public void testInnerHtml() {
        Assert.assertTrue(invoke("innerHtml") instanceof EvalError);
        Assert.assertTrue(invoke("innerHtml", "test") instanceof EvalError);

        EvalError evalError = (EvalError) invoke("innerHtml", "test");
        Assert.assertEquals(evalError.toString(),
                "innerHtml() cannot work with this \'string\'. The first parameter is not an HTML Element. Please first use parseHtml(string) and select(query) prior to using this function");
    }

    @Test
    public void testWholeText() {
        Assert.assertTrue(invoke("wholeText") instanceof EvalError);
        Assert.assertTrue(invoke("wholeText", "test") instanceof EvalError);

        EvalError evalError = (EvalError) invoke("wholeText", "test");
        Assert.assertEquals(evalError.toString(),
                "wholeText() cannot work with this \'string\' and failed as the first parameter is not an XML or HTML Element.  Please first use parseXml() or parseHtml() and select(query) prior to using this function");
    }

    @Test
    public void testXmlText() {
        Assert.assertTrue(invoke("xmlText") instanceof EvalError);
        Assert.assertTrue(invoke("xmlText", "test") instanceof EvalError);

        EvalError evalError = (EvalError) invoke("xmlText", "test");
        Assert.assertEquals(evalError.toString(),
                "xmlText() cannot work with this \'string\' and failed as the first parameter is not an XML or HTML Element.  Please first use parseXml() or parseHtml() and select(query) prior to using this function");
    }
}
