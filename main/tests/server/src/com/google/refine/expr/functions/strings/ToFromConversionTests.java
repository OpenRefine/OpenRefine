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

package com.google.refine.expr.functions.strings;

import java.time.OffsetDateTime;
import java.util.Properties;
import java.util.TimeZone;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.util.CalendarParser;
import com.google.refine.expr.util.CalendarParserException;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.util.ParsingUtilities;


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
        Assert.assertTrue(invoke("toNumber") instanceof EvalError);
        Assert.assertTrue(invoke("toNumber", (Object) null) instanceof EvalError);
        Assert.assertTrue(invoke("toNumber", "") instanceof EvalError);
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
      Assert.assertEquals(invoke("toString", (Object) null), "");
      Assert.assertEquals(invoke("toString", Long.valueOf(100)),"100");
      Assert.assertEquals(invoke("toString", Double.valueOf(100.0)),"100.0");
      Assert.assertEquals(invoke("toString", Double.valueOf(100.0),"%.0f"),"100");
      
      String inputDate = "2013-06-01";
      Assert.assertEquals(invoke("toString", CalendarParser.parseAsOffsetDateTime(inputDate)), 
              "2013-06-01T00:00:00Z");
      Assert.assertEquals(invoke("toString", CalendarParser.parseAsOffsetDateTime(inputDate), "yyyy-MM-dd"),
              "2013-06-01");
      Assert.assertEquals(invoke("toString", CalendarParser.parseAsOffsetDateTime(inputDate), "yyyy/dd/MM"), "2013/01/06");
      Assert.assertEquals(invoke("toString", CalendarParser.parseAsOffsetDateTime(inputDate), "yyyy-MM-dd hh:mm:ss"), "2013-06-01 12:00:00");
      
      String inputDateTime = "2013-06-01 13:12:11";
      Assert.assertEquals(invoke("toString", CalendarParser.parseAsOffsetDateTime(inputDateTime)), "2013-06-01T13:12:11Z");
      Assert.assertEquals(invoke("toString", CalendarParser.parseAsOffsetDateTime(inputDateTime), "yyyy-MM-dd"), "2013-06-01");
      Assert.assertEquals(invoke("toString", CalendarParser.parseAsOffsetDateTime(inputDateTime), "yyyy-MM-dd hh:mm:ss"),"2013-06-01 01:12:11");
      Assert.assertEquals(invoke("toString", CalendarParser.parseAsOffsetDateTime(inputDateTime), "yyyy-MM-dd HH:mm:ss"),"2013-06-01 13:12:11");
    }
    
    @Test
    public void testToDate() throws CalendarParserException {
      Assert.assertTrue(invoke("toDate") instanceof EvalError);
      Assert.assertTrue(invoke("toDate", (Object) null) instanceof EvalError);
      Assert.assertTrue(invoke("toDate", "") instanceof EvalError);
      Assert.assertTrue(invoke("toDate", 1.0) instanceof EvalError);
      //Assert.assertTrue(invoke("toDate", "2012-03-01","xxx") instanceof EvalError); // bad format string
      Assert.assertTrue(invoke("toDate", "2012-03-01") instanceof OffsetDateTime);
      Assert.assertEquals(invoke("toDate", "2012-03-01"),CalendarParser.parseAsOffsetDateTime("2012-03-01"));
      //parse as 'month first' date with and without explicit 'true' parameter
      Assert.assertEquals(invoke("toDate", "01/03/2012"),CalendarParser.parseAsOffsetDateTime("2012-01-03"));
      Assert.assertEquals(invoke("toDate", "01/03/2012",true),CalendarParser.parseAsOffsetDateTime("2012-01-03"));
      //parse as 'month first' date with 'false' parameter
      Assert.assertEquals(invoke("toDate", "01/03/2012",false),CalendarParser.parseAsOffsetDateTime("2012-03-01"));
      //parse as 'month first' date without 'false' parameter but with format specified
      Assert.assertEquals(invoke("toDate", "01/03/2012","dd/MM/yyyy"),CalendarParser.parseAsOffsetDateTime("2012-03-01"));
      Assert.assertEquals(invoke("toDate", "2012-03-01","yyyy-MM-dd"),CalendarParser.parseAsOffsetDateTime("2012-03-01"));
      //Two digit year
      Assert.assertEquals(invoke("toDate", "02-02-01"),CalendarParser.parseAsOffsetDateTime("2001-02-02"));
      // Multiple format strings should get tried sequentially until one succeeds or all are exhausted
      Assert.assertEquals(invoke("toDate", "2012-03-01","MMM","yyyy-MM-dd"), CalendarParser.parseAsOffsetDateTime("2012-03-01"));
      
      // Boolean argument combined with Multiple format strings 
      Assert.assertEquals(invoke("toDate", "01/03/2012",false, "MMM","yyyy-MM-dd","MM/dd/yyyy"), CalendarParser.parseAsOffsetDateTime("2012-03-01"));
      
      // First string can be a locale identifier instead of a format string
      Assert.assertEquals(invoke("toDate", "01-六月-2013","zh","dd-MMM-yyyy"), CalendarParser.parseAsOffsetDateTime("2013-06-01"));
      
      //if invalid format/locale strings are passed, ignore them
      Assert.assertEquals(invoke("toDate", "2012-03-01","XXX"), invoke("toDate", "2012-03-01"));

      // If a long, convert to string
      Assert.assertEquals(invoke("toDate", (long) 2012), invoke("toDate", "2012-01-01"));

      // If already a date, leave it alone
      Assert.assertEquals(invoke("toDate", CalendarParser.parseAsOffsetDateTime("2012-03-01")),CalendarParser.parseAsOffsetDateTime("2012-03-01"));
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
    
    @Test
    public void testUnescape() {
        Assert.assertEquals(invoke("unescape", "&Auml;", "html"),"Ä");
        Assert.assertEquals(invoke("unescape", "\\u00C4", "javascript"),"Ä");
    }

}
