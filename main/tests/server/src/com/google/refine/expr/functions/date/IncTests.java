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

package com.google.refine.expr.functions.date;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.util.TestUtils;

public class IncTests extends RefineTest {

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSSSSSSSSX");

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Test
    public void testInc() {
        OffsetDateTime source = OffsetDateTime.parse("20180510-23:55:44.000789000Z",
                formatter);

        // add hours
        Assert.assertTrue(invoke("inc", source, 2, "hours") instanceof OffsetDateTime);
        Assert.assertEquals(invoke("inc", source, 2, "hours"), source.plus(2, ChronoUnit.HOURS));
        Assert.assertEquals(invoke("inc", source, 2, "hour"), source.plus(2, ChronoUnit.HOURS));
        Assert.assertEquals(invoke("inc", source, 2, "h"), source.plus(2, ChronoUnit.HOURS));

        // add years
        Assert.assertTrue(invoke("inc", source, 2, "year") instanceof OffsetDateTime);
        Assert.assertEquals(invoke("inc", source, 2, "years"), source.plus(2, ChronoUnit.YEARS));
        Assert.assertEquals(invoke("inc", source, 2, "year"), source.plus(2, ChronoUnit.YEARS));

        // add months
        Assert.assertTrue(invoke("inc", source, 2, "months") instanceof OffsetDateTime);
        Assert.assertEquals(invoke("inc", source, 2, "months"), source.plus(2, ChronoUnit.MONTHS));
        Assert.assertEquals(invoke("inc", source, 2, "month"), source.plus(2, ChronoUnit.MONTHS));

        // add minutes
        Assert.assertTrue(invoke("inc", source, 2, "minutes") instanceof OffsetDateTime);
        Assert.assertEquals(invoke("inc", source, 2, "minutes"), source.plus(2, ChronoUnit.MINUTES));
        Assert.assertEquals(invoke("inc", source, 2, "minute"), source.plus(2, ChronoUnit.MINUTES));
        Assert.assertEquals(invoke("inc", source, 2, "min"), source.plus(2, ChronoUnit.MINUTES));

        // add weeks
        Assert.assertTrue(invoke("inc", source, 2, "weeks") instanceof OffsetDateTime);
        Assert.assertEquals(invoke("inc", source, 2, "weeks"), source.plus(2, ChronoUnit.WEEKS));
        Assert.assertEquals(invoke("inc", source, 2, "week"), source.plus(2, ChronoUnit.WEEKS));
        Assert.assertEquals(invoke("inc", source, 2, "w"), source.plus(2, ChronoUnit.WEEKS));

        // add seconds
        Assert.assertTrue(invoke("inc", source, 2, "seconds") instanceof OffsetDateTime);
        Assert.assertEquals(invoke("inc", source, 2, "seconds"), source.plus(2, ChronoUnit.SECONDS));
        Assert.assertEquals(invoke("inc", source, 2, "sec"), source.plus(2, ChronoUnit.SECONDS));
        Assert.assertEquals(invoke("inc", source, 2, "s"), source.plus(2, ChronoUnit.SECONDS));

        // add milliseconds
        Assert.assertTrue(invoke("inc", source, 2, "milliseconds") instanceof OffsetDateTime);
        Assert.assertEquals(invoke("inc", source, 2, "milliseconds"), source.plus(2, ChronoUnit.MILLIS));
        Assert.assertEquals(invoke("inc", source, 2, "ms"), source.plus(2, ChronoUnit.MILLIS));
        Assert.assertEquals(invoke("inc", source, 2, "S"), source.plus(2, ChronoUnit.MILLIS));

        // add nanos
        Assert.assertTrue(invoke("inc", source, 2, "nanos") instanceof OffsetDateTime);
        Assert.assertEquals(invoke("inc", source, 2, "nanos"), source.plus(2, ChronoUnit.NANOS));
        Assert.assertEquals(invoke("inc", source, 2, "nano"), source.plus(2, ChronoUnit.NANOS));
        Assert.assertEquals(invoke("inc", source, 2, "n"), source.plus(2, ChronoUnit.NANOS));

        // exception
        Assert.assertTrue(invoke("inc", source, 99) instanceof EvalError);
        Assert.assertTrue(invoke("inc", source.toInstant().toEpochMilli(), 99, "h") instanceof EvalError);
    }

}
