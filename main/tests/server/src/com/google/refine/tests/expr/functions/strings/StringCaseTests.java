/*

Copyright 2011, Thomas F. Morris
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
 * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

package com.google.refine.tests.expr.functions.strings;

import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.functions.strings.ToTitlecase;
import com.google.refine.grel.Function;
import com.google.refine.tests.RefineTest;


public class StringCaseTests extends RefineTest {

    static Properties bindings;

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
    
    @Test
    public void testToTitlecaseInvalidParams() {        
        Function fut = new ToTitlecase();
        Assert.assertTrue(fut.call(bindings, new Object[]{}) instanceof EvalError);
        Assert.assertTrue(fut.call(bindings, new Object[]{"one","two"}) instanceof EvalError);
        Assert.assertTrue(fut.call(bindings, new Object[]{"one","two","three"}) instanceof EvalError);
    }
    
    @Test
    public void testToTitlecase() {        
        Function fut = new ToTitlecase();
        Assert.assertEquals((String)(fut.call(bindings, new Object[]{"one"})),"One");
        Assert.assertEquals((String)(fut.call(bindings, new Object[]{"ONE"})),"One");
        Assert.assertEquals((String)(fut.call(bindings, new Object[]{"one two three"})),"One Two Three");
        Assert.assertEquals((String)(fut.call(bindings, new Object[]{"C.R. SANDIDGE WINES, INC."})),"C.R. Sandidge Wines, Inc.");
//        Assert.assertEquals((String)(fut.call(bindings, new Object[]{"C.R. SANDIDGE WINES, INC.",",. "})),"C.R. Sandidge Wines, Inc.");
//        Assert.assertEquals((String)(fut.call(bindings, new Object[]{"one-two-three","-"})),"One-Two-Three");
    }
    
}
