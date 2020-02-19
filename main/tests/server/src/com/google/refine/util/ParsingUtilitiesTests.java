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

package com.google.refine.util;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.util.ParsingUtilities;

public class ParsingUtilitiesTests extends RefineTest {
    
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
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
    public void stringToDate() {
        Assert.assertEquals(2017, ParsingUtilities.stringToDate("2017-04-03T08:09:43.123").getYear());
        Assert.assertEquals(2017, ParsingUtilities.stringToDate("2017-04-03T08:09:43").getYear());
        Assert.assertEquals(2017, ParsingUtilities.stringToDate("2017-04-03T08:09:43Z").getYear());
        Assert.assertEquals(2017, ParsingUtilities.stringToDate("2017-04-03T08:09:43.123Z").getYear());
        Assert.assertEquals(2017, ParsingUtilities.stringToDate("2017-04-03T08:09:43+00:00").getYear());
    }
    
    @Test
    public void stringToLocalDate() {
        Assert.assertEquals(2017, ParsingUtilities.stringToLocalDate("2017-04-03T08:09:43.123").getYear());
        Assert.assertEquals(2017, ParsingUtilities.stringToLocalDate("2017-04-03T08:09:43").getYear());
        Assert.assertEquals(2017, ParsingUtilities.stringToLocalDate("2017-04-03T08:09:43Z").getYear());
        Assert.assertEquals(2017, ParsingUtilities.stringToLocalDate("2017-04-03T08:09:43.123Z").getYear());
        Assert.assertEquals(2017, ParsingUtilities.stringToLocalDate("2017-04-03T08:09:43+00:00").getYear());
    }
    
    /**
     * Converting between string and local time must be reversible, no matter the timezone.
     */
    @Test
    public void stringToLocalDateNonUTC() {
    	TimeZone.setDefault(TimeZone.getTimeZone("JST"));
    	try {
    		Assert.assertEquals(ParsingUtilities.stringToLocalDate("2001-08-12T00:00:00Z").getHour(), 9);
    		Assert.assertEquals(ParsingUtilities.localDateToString(
    				ParsingUtilities.stringToLocalDate("2001-08-12T00:00:00Z")),
    				"2001-08-12T00:00:00Z");
    		
    	} finally {
    		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    	}
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
