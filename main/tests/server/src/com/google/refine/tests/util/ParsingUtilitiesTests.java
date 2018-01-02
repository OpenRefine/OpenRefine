/*

Copyright 2010, Google Inc.
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

package com.google.refine.tests.util;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.tests.RefineTest;
import com.google.refine.util.ParsingUtilities;

public class ParsingUtilitiesTests extends RefineTest {
    
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }
    
    //--------------evaluateJsonStringToObject tests-----------------------

    @Test
    public void evaluateJsonStringToObjectRegressionTest(){
        try {
            JSONObject o = ParsingUtilities.evaluateJsonStringToObject("{\"foo\":\"bar\"}");
            Assert.assertNotNull(o);
            Assert.assertEquals("bar", o.getString("foo"));
        } catch (JSONException e) {
            Assert.fail();
        }
    }

    @Test
    public void evaluateJsonStringToObjectWithNullParameters(){
        try {
            Assert.assertNull(ParsingUtilities.evaluateJsonStringToObject(null));
            Assert.fail();
        } catch (IllegalArgumentException e){
            //expected
        } catch (JSONException e) {
            Assert.fail();
        }
    }

    @Test
    public void evaluateJsonStringToObjectWithMalformedParameters(){
        try {
            ParsingUtilities.evaluateJsonStringToObject("malformed");
            Assert.fail();
        } catch (JSONException e) {
            //expected
        }
    }
    @Test
    public void zonedDateTimeTest() {
        String  d = "2017-12-01T14:53:36Z";
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        OffsetDateTime.parse(d, formatter);
    }
    
    @Test
    public void parseProjectBeforeJDK8() {
        String historyEntryDate = "2017-12-01T14:53:36Z";
        
        OffsetDateTime zdt = ParsingUtilities.stringToDate(historyEntryDate);
        String zdtString = ParsingUtilities.dateToString(zdt);
        Assert.assertEquals(zdtString, historyEntryDate);
        
    }
    
    @Test
    public void parseProjectModifiedBeforeJDK8() {
        String modified = "2014-01-15T21:46:25Z";
        Assert.assertNotEquals(ParsingUtilities.stringToLocalDate(modified).toString(), 
                modified);
    }
    
    @Test
    public void strSubstitutorTest() {
        Map<String, String> data = new HashMap<String, String>(6);
        
        data.put("value", "1234");
        data.put("field_format", "String");
        
        StrSubstitutor sub = new StrSubstitutor(data);
        String message = "The value ${value} in row ${row_number} and column ${column_number} is not type ${field_type} and format ${field_format}";
        String result = sub.replace(message);
        Assert.assertTrue(result.contains("1234"));
    }
}
