/*

Copyright 2012, Thomas F. Morris
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

import java.text.DateFormat;
import java.util.GregorianCalendar;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.util.CalendarParser;
import com.google.refine.expr.util.CalendarParserException;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.tests.RefineTest;


/**
 * Tests for string up/low/title case functions.  
 * (A very brief start so far)
 * 
 * @author Tom Morris <tfmorris@gmail.com>
 */
public class ToFromConversionTests extends RefineTest {

    private static final double EPSILON = 0.0001;
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
    public void testToNumber() {
//        Assert.assertTrue(invoke("toNumber") instanceof EvalError);
        Assert.assertNull(invoke("toNumber"));
//        Assert.assertTrue(invoke("toNumber", (Object) null) instanceof EvalError);
        Assert.assertNull(invoke("toNumber", (Object) null));
        Assert.assertTrue(invoke("toNumber", "string") instanceof EvalError);
        Assert.assertEquals(invoke("toNumber", "0.0"), 0.0);
        Assert.assertEquals(invoke("toNumber", "123"), Long.valueOf(123));
        Assert.assertTrue(Math.abs((Double) invoke("toNumber", "123.456") - 123.456) < EPSILON);
        Assert.assertTrue(Math.abs((Double) invoke("toNumber", "001.234") - 1.234) < EPSILON);
        Assert.assertTrue(Math.abs((Double) invoke("toNumber", "1e2") - 100.0) < EPSILON);
        Assert.assertTrue(Math.abs((Double) invoke("toNumber", Double.parseDouble("100.0")) - 100.0) < EPSILON);
    }

    @Test
    public void testToString() throws CalendarParserException {
      Assert.assertTrue(invoke("toString") instanceof EvalError);
      Assert.assertEquals(invoke("toString", (Object) null), "null");
      Assert.assertEquals(invoke("toString", Long.valueOf(100)),"100");
      Assert.assertEquals(invoke("toString", Double.valueOf(100.0)),"100.0");
      Assert.assertEquals(invoke("toString", Double.valueOf(100.0),"%.0f"),"100");
      
      String expectedDate = DateFormat.getDateInstance().format(new GregorianCalendar(2013,5,1).getTime());
      Assert.assertEquals(invoke("toString", CalendarParser.parse("2013-06-01")), expectedDate);
      Assert.assertEquals(invoke("toString", CalendarParser.parse("2013-06-01").getTime()), expectedDate);
      Assert.assertEquals(invoke("toString", CalendarParser.parse("2013-06-01"),"yyyy"),"2013");      
      Assert.assertEquals(invoke("toString", CalendarParser.parse("2013-06-01"),"yyyy-MM-dd"),"2013-06-01");
    }
    
    @Test
    public void testToDate() throws CalendarParserException {
//      Assert.assertTrue(invoke("toDate") instanceof EvalError);
      Assert.assertNull(invoke("toDate"));
      Assert.assertTrue(invoke("toDate", (Object) null) instanceof EvalError);
      Assert.assertTrue(invoke("toDate", 1.0) instanceof EvalError);
      Assert.assertTrue(invoke("toDate", "2012-03-01","xxx") instanceof EvalError); // bad format string
      Assert.assertTrue(invoke("toDate", "2012-03-01") instanceof GregorianCalendar);
      Assert.assertEquals(invoke("toDate", "2012-03-01"),CalendarParser.parse("2012-03-01"));
      Assert.assertEquals(invoke("toDate", "2012-03-01","yyyy-MM-dd"),CalendarParser.parse("2012-03-01"));
      // Multiple format strings should get tried sequentially until one succeeds or all are exhausted
      Assert.assertEquals(invoke("toDate", "2012-03-01","MMM","yyyy-MM-dd"), CalendarParser.parse("2012-03-01"));
      // First string can be a locale identifier instead of a format string
      Assert.assertEquals(invoke("toDate", "2013-06-01","zh"), CalendarParser.parse("2013-06-01")); 
      Assert.assertEquals(invoke("toDate", "01-六月-2013","zh","dd-MMM-yyyy"), CalendarParser.parse("2013-06-01")); 
      Assert.assertEquals(invoke("toDate", "01-六月-2013","zh_HK","dd-MMM-yyyy"), CalendarParser.parse("2013-06-01")); 
      Assert.assertEquals(invoke("toDate", "01-六月-2013","zh_TW","dd-MMM-yyyy"), CalendarParser.parse("2013-06-01")); 
      Assert.assertEquals(invoke("toDate", "01-六月-2013","zh_CN","dd-MMM-yyyy"), CalendarParser.parse("2013-06-01")); 

        // Date
        // Calendar
        // String
    }
    
    @Test
    public void testEscape() {
      Assert.assertNull(invoke("escape"));
      Assert.assertEquals(invoke("escape",null,"xml"),"");
      Assert.assertEquals(invoke("escape", "mystring", "html"),"mystring");
      Assert.assertEquals(invoke("escape", "mystring", "xml"),"mystring");
      Assert.assertEquals(invoke("escape", "mystring", "csv"),"mystring");
      Assert.assertEquals(invoke("escape", "mystring", "url"),"mystring");
      Assert.assertEquals(invoke("escape", "mystring", "javascript"),"mystring");
      Assert.assertEquals(invoke("escape", 1, "html"),"1");
      Assert.assertEquals(invoke("escape", 1, "xml"),"1");
      Assert.assertEquals(invoke("escape", 1, "csv"),"1");
      Assert.assertEquals(invoke("escape", 1, "url"),"1");
      Assert.assertEquals(invoke("escape", 1, "javascript"),"1");
      Assert.assertEquals(invoke("escape", Long.parseLong("1"), "html"),"1");
      Assert.assertEquals(invoke("escape", Long.parseLong("1"), "xml"),"1");
      Assert.assertEquals(invoke("escape", Long.parseLong("1"), "csv"),"1");
      Assert.assertEquals(invoke("escape", Long.parseLong("1"), "url"),"1");
      Assert.assertEquals(invoke("escape", Long.parseLong("1"), "javascript"),"1");
      Assert.assertEquals(invoke("escape", Double.parseDouble("1.23"), "html"),"1.23");
      Assert.assertEquals(invoke("escape", Double.parseDouble("1.23"), "xml"),"1.23");
      Assert.assertEquals(invoke("escape", Double.parseDouble("1.23"), "csv"),"1.23");
      Assert.assertEquals(invoke("escape", Double.parseDouble("1.23"), "url"),"1.23");
      Assert.assertEquals(invoke("escape", Double.parseDouble("1.23"), "javascript"),"1.23");
   }

}
