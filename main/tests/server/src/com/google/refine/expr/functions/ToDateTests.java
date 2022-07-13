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

package com.google.refine.expr.functions;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.OffsetDateTime;
import java.util.TimeZone;

import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.util.CalendarParser;
import com.google.refine.expr.util.CalendarParserException;
import com.google.refine.util.TestUtils;

public class ToDateTests extends RefineTest {

    @Test
    public void testToDate() throws CalendarParserException {
        TimeZone originalTimeZone = TimeZone.getDefault();
        try {
            // Inject a fixed non-UTC timezone
            TimeZone.setDefault(TimeZone.getTimeZone("JST"));

            assertTrue(invoke("toDate") instanceof EvalError);
            assertTrue(invoke("toDate", (Object) null) instanceof EvalError);
            assertTrue(invoke("toDate", "") instanceof EvalError);
            assertTrue(invoke("toDate", 1.0) instanceof EvalError);
            assertTrue(invoke("toDate", "2012-03-01", "xxx") instanceof EvalError); // bad format string
            assertTrue(invoke("toDate", "2012-03-01", 1L) instanceof EvalError); // non-string format arg
            assertTrue(invoke("toDate", "P1M") instanceof EvalError); // Durations aren't supported

            assertTrue(invoke("toDate", "2012-03-01") instanceof OffsetDateTime);
            assertEquals(invoke("toDate", "2012-03-01"), CalendarParser.parseAsOffsetDateTime("2012-03-01"));
            // parse as 'month first' date with and without explicit 'true' parameter
            assertEquals(invoke("toDate", "01/03/2012"), CalendarParser.parseAsOffsetDateTime("2012-01-03"));
            assertEquals(invoke("toDate", "01/03/2012", true), CalendarParser.parseAsOffsetDateTime("2012-01-03"));
            // parse as 'month first' date with 'false' parameter
            assertEquals(invoke("toDate", "01/03/2012", false), CalendarParser.parseAsOffsetDateTime("2012-03-01"));
            // parse as 'month first' date without 'false' parameter but with format specified
            assertEquals(invoke("toDate", "01/03/2012", "dd/MM/yyyy"), CalendarParser.parseAsOffsetDateTime("2012-03-01"));
            assertEquals(invoke("toDate", "2012-03-01", "yyyy-MM-dd"), CalendarParser.parseAsOffsetDateTime("2012-03-01"));
            // Two digit year
            assertEquals(invoke("toDate", "02-02-01"), CalendarParser.parseAsOffsetDateTime("2001-02-02"));
            // Multiple format strings should get tried sequentially until one succeeds or all are exhausted
            assertEquals(invoke("toDate", "2012-03-01", "MMM", "yyyy-MM-dd"), CalendarParser.parseAsOffsetDateTime("2012-03-01"));

            // Boolean argument combined with Multiple format strings
            assertEquals(invoke("toDate", "01/03/2012", false, "MMM", "yyyy-MM-dd", "MM/dd/yyyy"),
                    CalendarParser.parseAsOffsetDateTime("2012-03-01"));

            // First string can be a locale identifier instead of a format string
            assertEquals(invoke("toDate", "2013-06-01", "zh"), CalendarParser.parseAsOffsetDateTime("2013-06-01"));
            assertEquals(invoke("toDate", "01-六月-2013", "zh", "dd-MMM-yyyy"), CalendarParser.parseAsOffsetDateTime("2013-06-01"));
            assertEquals(invoke("toDate", "01-六月-2013", "zh-CN", "dd-MMM-yyyy"), CalendarParser.parseAsOffsetDateTime("2013-06-01"));
            assertEquals(invoke("toDate", "01-六月-2013", "zh", "MMM-dd-yyyy", "dd-MMM-yyyy"),
                    CalendarParser.parseAsOffsetDateTime("2013-06-01"));

            // If a long, convert to string
            assertEquals(invoke("toDate", (long) 2012), invoke("toDate", "2012-01-01"));

            // If already a date, leave it alone
            assertEquals(invoke("toDate", CalendarParser.parseAsOffsetDateTime("2012-03-01")),
                    CalendarParser.parseAsOffsetDateTime("2012-03-01"));

            // FIXME: Date/times without timezone info were interpreted as local up until May 2018 at which point they
            // switch to UTC
            // assertEquals(invoke("toDate", "2013-06-01T13:12:11"), CalendarParser.parseAsOffsetDateTime("2013-06-01
            // 13:12:11"));

            // These match current behavior, but would fail with the historic (pre-2018) behavior
            assertEquals(invoke("toDate", "2013-06-01T13:12:11Z"), CalendarParser.parseAsOffsetDateTime("2013-06-01 13:12:11"));
            assertEquals(invoke("toDate", "2013-06-01Z"), CalendarParser.parseAsOffsetDateTime("2013-06-01"));

            // TODO: more tests for datetimes with timezones and/or offsets
            // assertEquals(invoke("toDate", "2013-06-01T13:12:11+06:00"),
            // CalendarParser.parseAsOffsetDateTime("2013-06-01 13:12:11"));
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

}
