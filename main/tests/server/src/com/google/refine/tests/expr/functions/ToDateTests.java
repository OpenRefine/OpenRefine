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
package com.google.refine.tests.expr.functions;

import com.google.refine.tests.RefineTest;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.expr.functions.ToDate;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Properties;

public class ToDateTests extends RefineTest {

    static Properties bindings;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Test
    public void testUnixTime()
    {
        Assert.assertEquals((OffsetDateTime)invoke("toDate", 123456l, "unix"), OffsetDateTime.parse("1970-01-02T10:17:36+00:00"));
        Assert.assertEquals((OffsetDateTime)invoke("toDate", -123456l, "unix"), OffsetDateTime.parse("1969-12-30T13:42:24+00:00"));
        Assert.assertEquals((OffsetDateTime)invoke("toDate", 123456, "unix"), OffsetDateTime.parse("1970-01-02T10:17:36+00:00"));
        Assert.assertEquals((OffsetDateTime)invoke("toDate", 1, "unix"), OffsetDateTime.parse("1970-01-01T00:00:01+00:00"));
        Assert.assertEquals((OffsetDateTime)invoke("toDate", 0, "unix"), OffsetDateTime.parse("1970-01-01T00:00:00+00:00"));
        Assert.assertEquals((OffsetDateTime)invoke("toDate", 2147483647, "unix"), OffsetDateTime.parse("2038-01-19T03:14:07+00:00"));
    }
    
    @Test
    public void testOffsetDateTime()
    {
        Assert.assertEquals(invoke("toDate", OffsetDateTime.of(2019,06,19,12,21,22,33,ZoneOffset.MAX)), OffsetDateTime.of(2019,06,19,12,21,22,33,ZoneOffset.MAX));    
    }
    
    @Test
    public void testStrings()
    {
        Assert.assertEquals((OffsetDateTime)invoke("toDate", "2019-06-19T12:21:22.330"+ZoneOffset.MAX), OffsetDateTime.parse("2019-06-19T12:21:22.330"+ZoneOffset.MAX));
        Assert.assertEquals((OffsetDateTime)invoke("toDate", "06-19-2019", true), OffsetDateTime.parse("2019-06-19T00:00:00.00"+ZoneOffset.UTC));
        Assert.assertEquals((OffsetDateTime)invoke("toDate", "2019-06-19", false), OffsetDateTime.parse("2019-06-19T00:00:00.00"+ZoneOffset.UTC));
        Assert.assertEquals((OffsetDateTime)invoke("toDate", "02001.July.04 AD 12:08 PM", "yyyyy.MMMMM.dd GGG hh:mm aaa"), OffsetDateTime.parse("2001-07-04T12:08:00.00"+ZoneOffset.UTC));
        Assert.assertEquals((OffsetDateTime)invoke("toDate", "02001.July.04 AD 12:08 PM", "yyyyy.MMMMM.dd GGG hh:mm aaa", "1234"), OffsetDateTime.parse("2001-07-04T12:08:00.00"+ZoneOffset.UTC));
        Assert.assertEquals((OffsetDateTime)invoke("toDate", "02001.July.04 AD 12:08 PM", "yyyyy.MMMMM.dd GyGyG hh:mm aaa", "yyyyy.MMMMM.dd GGG hh:mm aaa", "1234"),  OffsetDateTime.parse("2001-07-04T12:08:00.00"+ZoneOffset.UTC));       
    }
    
    @Test
    public void testErrors()
    {
        Assert.assertTrue(invoke("toDate", new Object()) instanceof EvalError);
        Assert.assertTrue(invoke("toDate", "1234", new Object(), new Object()) instanceof EvalError);
        Assert.assertTrue(invoke("toDate", "asddsa", "yyyy.mm.dd", new Object()) instanceof EvalError);
        Assert.assertTrue(invoke("toDate") instanceof EvalError);
        Assert.assertTrue(invoke("toDate", "02001.July.04 AD 12:08 PM", "yyyyy.MMMMM.dd GyGyG hh:mm aaa", "1234") instanceof EvalError);
    }

    @Test
    public void serializeToDate() {
        String json = "{\"description\":\"Returns o converted to a date object, you can hint if the day or the month is listed first, use unix time (with \\\"unix\\\" flag) or give an ordered list of possible formats using this syntax: http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html\",\"params\":\"o / integer unix_seconds (only with unix flag), boolean month_first / \\\"unix\\\" / format1, format2, ... (all optional) \",\"returns\":\"date\"}";
        TestUtils.isSerializedTo(new ToDate(), json);
    }

    @BeforeMethod
    public void SetUp() {
        bindings = new Properties();
    }

    @AfterMethod
    public void TearDown() {
        bindings = null;
    }

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
}

