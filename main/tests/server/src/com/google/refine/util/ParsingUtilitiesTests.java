/*

Copyright 2010, Google Inc.
Copyright 2013,2020 OpenRefine contributors

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;

public class ParsingUtilitiesTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Test
    public void zonedDateTimeTest() {
        String d = "2017-12-01T14:53:36Z";
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
        TimeZone originalTimeZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("JST"));
            Assert.assertEquals(ParsingUtilities.stringToLocalDate("2001-08-12T00:00:00Z").getHour(), 9);
            // TODO: This doesn't really make sense since a LocalDate, by definition, doesn't have timezone info
            Assert.assertEquals(ParsingUtilities.localDateToString(
                    ParsingUtilities.stringToLocalDate("2001-08-12T00:00:00Z")),
                    "2001-08-12T00:00:00Z");

        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    @Test
    public void testParseGZIPInutstream() throws IOException {
        // Test decompressing gzip
        try {
            String sampleBody = "<HTML>\n" +
                    "\n" +
                    "<HEAD>\n" +
                    "\n" +
                    "<TITLE>Your Title Here</TITLE>\n" +
                    "\n" +
                    "</HEAD>\n" +
                    "\n" +
                    "<BODY BGCOLOR=\"FFFFFF\">\n" +
                    "\n" +
                    "</BODY>\n" +
                    "\n" +
                    "</HTML>";
            ByteArrayOutputStream obj = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(obj);
            gzip.write(sampleBody.getBytes("UTF-8"));
            gzip.close();
            byte[] compressed = obj.toByteArray();

            String res = ParsingUtilities.inputStreamToString(new ByteArrayInputStream(compressed), "gzip");
            Assert.assertEquals(res, sampleBody);
        } catch (Exception e) {
            Assert.fail();
        }
    }
}
