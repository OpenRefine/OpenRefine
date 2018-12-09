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
package com.google.refine.tests.expr.functions.strings;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.functions.strings.Diff;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;


public class DiffTests extends RefineTest {

    static Properties bindings;
    
    private OffsetDateTime odt1;
    private OffsetDateTime odt2;
    
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
        odt1 = OffsetDateTime.parse("2011-09-01T10:15:30.123456+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        odt2 = OffsetDateTime.parse("2011-12-02T10:16:30.123467+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
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
    public void testDiffInvalidParams() {        
        Assert.assertTrue(invoke("diff") instanceof EvalError);
        Assert.assertTrue(invoke("diff", "one","two","three") instanceof EvalError);
    }
    
    @Test
    public void testDiffString() {        
        Assert.assertEquals((String)(invoke("diff", "onetwo","onetwothree")),"three");
    }    
     
    @Test
    public void testDiffOffsetDateTime() {  
        // OffsetDateTime diff:
        Assert.assertEquals(invoke("diff",odt2,odt1,"days"),Long.valueOf(92));
        Assert.assertEquals(invoke("diff",odt2,odt1,"weeks"),Long.valueOf(13));
        Assert.assertEquals(invoke("diff",odt2,odt1,"months"),Long.valueOf(3));
        Assert.assertEquals(invoke("diff",odt2,odt1,"hours"),Long.valueOf(2208));
        Assert.assertEquals(invoke("diff",odt2,odt1,"seconds"),Long.valueOf(7948860));
        Assert.assertEquals(invoke("diff",odt2,odt1,"milliseconds"),Long.valueOf(7948860000011l));
        Assert.assertEquals(invoke("diff",odt2,odt1,"nanos"),Long.valueOf(7948860000011000l));
    }
    
    @Test
    public void serializeDiff() {
        String json = "{\"description\":\"For strings, returns the portion where they differ. For dates, it returns the difference in given time units\",\"params\":\"o1, o2, time unit (optional)\",\"returns\":\"string for strings, number for dates\"}";
        TestUtils.isSerializedTo(new Diff(), json);
    }

}
