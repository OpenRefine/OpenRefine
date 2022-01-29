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

package com.google.refine.expr.functions.strings;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.util.TestUtils;

public class DiffTests extends RefineTest {

    static Properties bindings;

    private OffsetDateTime odt1;
    private OffsetDateTime odt2;
    private OffsetDateTime odt3;
    private OffsetDateTime odt4;
    private OffsetDateTime odt5;
    private OffsetDateTime odt6;
    private OffsetDateTime odt7;
    private OffsetDateTime odt8;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
        odt1 = OffsetDateTime.parse("2011-09-01T10:15:30.123456+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        odt2 = OffsetDateTime.parse("2011-12-02T10:16:30.123467+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        odt3 = OffsetDateTime.of(2010, 1, 10, 12, 0, 0, 0, ZoneOffset.UTC);
        odt4 = OffsetDateTime.of(2999, 1, 10, 12, 0, 0, 0, ZoneOffset.UTC);
        odt5 = OffsetDateTime.of(2285, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        odt6 = OffsetDateTime.of(1700, 1, 10, 12, 0, 0, 0, ZoneOffset.UTC);
        odt7 = OffsetDateTime.of(1923, 4, 19, 12, 0, 0, 0, ZoneOffset.UTC);
        odt8 = OffsetDateTime.of(1923, 4, 21, 12, 0, 0, 0, ZoneOffset.UTC);
    }

    @Test
    public void testDiffInvalidParams() {
        Assert.assertTrue(invoke("diff") instanceof EvalError);
        Assert.assertTrue(invoke("diff", "one", "two", "three") instanceof EvalError);
    }

    @Test
    public void testDiffString() {
        Assert.assertEquals((String) (invoke("diff", "onetwo", "onetwothree")), "three");
    }

    @Test
    public void testDiffOffsetDateTime() {
        // OffsetDateTime diff:
        Assert.assertEquals(invoke("diff", odt2, odt3, "years"), Long.valueOf(1));
        Assert.assertEquals(invoke("diff", odt2, odt1, "days"), Long.valueOf(92));
        Assert.assertEquals(invoke("diff", odt2, odt1, "weeks"), Long.valueOf(13));
        Assert.assertEquals(invoke("diff", odt2, odt1, "months"), Long.valueOf(3));
        Assert.assertEquals(invoke("diff", odt2, odt1, "hours"), Long.valueOf(2208));
        Assert.assertEquals(invoke("diff", odt2, odt1, "minutes"), Long.valueOf(132481));
        Assert.assertEquals(invoke("diff", odt2, odt1, "seconds"), Long.valueOf(7948860));
        // Changed due to an error in previous version- it was checking for microseconds (1,000,000th of second) instead
        // of milliseconds (1000th of second)
        Assert.assertEquals(invoke("diff", odt2, odt1, "milliseconds"), Long.valueOf(7948860000l));
        Assert.assertEquals(invoke("diff", odt2, odt1, "nanos"), Long.valueOf(7948860000011000l));
    }

    @Test
    public void testDiffOffsetDateTimeEvalErrors() {
        // Check whether EvalError is returned if time in nanoseconds between two dates is larger than long max in java
        // when nanoseconds are desired unit.
        Assert.assertTrue(invoke("diff", odt3, odt4, "nanos") instanceof EvalError);
        Assert.assertTrue(invoke("diff", odt4, odt5, "nanos") instanceof EvalError);
        // At the same time if different time unit is requested for the same period, it should not result in EvalError.
        Assert.assertEquals(invoke("diff", odt3, odt4, "milliseconds"), Long.valueOf(-31209840000000l));
        Assert.assertEquals(invoke("diff", odt4, odt5, "milliseconds"), Long.valueOf(22532428800000l));
        Assert.assertEquals(invoke("diff", odt3, odt4, "days"), Long.valueOf(-361225));
        Assert.assertEquals(invoke("diff", odt4, odt5, "days"), Long.valueOf(260792));
        // Also, ensure that periods longer (in nanoseconds) than long max preserve continuity when expressed in
        // different time unit, like days.
        // Example: 1923-04-19 to 1700-01-01 is just below long max when expressed in nanoseconds
        Assert.assertEquals(invoke("diff", odt6, odt7, "days"), Long.valueOf(-81547));
        // and 1923-04-21 to 1700-01-01 is slightly above long max when expressed in nanoseconds
        Assert.assertEquals(invoke("diff", odt6, odt8, "days"), Long.valueOf(-81549));
    }

    @Test
    public void testDiffOffsetDateTimeEvalErrorsForIncorrectTimeUnit() {
        // In case if incorrect time unit is passed to function, EvalError should be returned
        Assert.assertTrue(invoke("diff", odt4, odt5, "milis") instanceof EvalError);
        Assert.assertTrue(invoke("diff", odt4, odt5, "millis") instanceof EvalError);
        Assert.assertTrue(invoke("diff", odt4, odt5, "yars") instanceof EvalError);
    }

}
