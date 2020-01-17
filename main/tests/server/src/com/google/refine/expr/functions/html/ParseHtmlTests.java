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
package com.google.refine.expr.functions.html;

import org.jsoup.Jsoup;
import org.testng.annotations.Test;

import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.functions.html.ParseHtml;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.util.TestUtils;

public class ParseHtmlTests extends RefineTest  {
    
    static Properties bindings;
    static String h =   "<html>\n" +
                        "<head>\n" +
                        "</head>\n" +
                        "    <body>\n" +
                        "        <h1>head1</h1>\n" +
                        "        <div class=\"class1\">\n" +
                        "            <p>para1 <strong>strong text</strong></p>\n" +
                        "            <p>para2</p>\n" +
                        "        </div>\n" +
                        "    </body>\n" +
                        "</html>";
    
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void SetUp() {
        bindings = new Properties();
    }

    @AfterMethod
    public void TearDown() {
        bindings = null;
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
    public void serializeParseHtml() {
        String json = "{\"description\":\"Parses a string as HTML\",\"params\":\"string s\",\"returns\":\"HTML object\"}";
        TestUtils.isSerializedTo(new ParseHtml(), json);
    }
    
    @Test
    public void testParseHtml() {
        Assert.assertTrue(invoke("parseHtml") instanceof EvalError);
        Assert.assertTrue(invoke("parseHtml","h") instanceof org.jsoup.nodes.Document);
        Assert.assertTrue(invoke("select",Jsoup.parse(h),"p") instanceof org.jsoup.select.Elements);
        Assert.assertTrue(invoke("innerHtml",Jsoup.parse(h).select("p").first()) instanceof String);
        Assert.assertEquals(invoke("innerHtml",Jsoup.parse(h).select("p").first()),"para1 <strong>strong text</strong>");
        Assert.assertEquals(invoke("htmlAttr",Jsoup.parse(h).select("div").first(),"class"),"class1");
        Assert.assertEquals(invoke("htmlText",Jsoup.parse(h).select("div").first()),"para1 strong text para2");
        Assert.assertEquals(invoke("ownText",Jsoup.parse(h).select("p").first()),"para1");
    }
}

