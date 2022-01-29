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
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.util.TestUtils;

public class DatePartTests extends RefineTest {

    static Properties bindings;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSSSSSSSSX");

    @Test
    public void testOffsetDateTimeDatePart() {
        // 2018-4-30 23:55:44
        OffsetDateTime source = OffsetDateTime.parse("20180430-23:55:44.000789000Z",
                formatter);

        // hours
        Assert.assertEquals(invoke("datePart", source, "hours"), 23);
        Assert.assertEquals(invoke("datePart", source, "hour"), 23);
        Assert.assertEquals(invoke("datePart", source, "h"), 23);

        // minutes
        Assert.assertEquals(invoke("datePart", source, "minutes"), 55);
        Assert.assertEquals(invoke("datePart", source, "minute"), 55);
        Assert.assertEquals(invoke("datePart", source, "min"), 55);

        // seconds
        Assert.assertEquals(invoke("datePart", source, "seconds"), 44);
        Assert.assertEquals(invoke("datePart", source, "sec"), 44);
        Assert.assertEquals(invoke("datePart", source, "s"), 44);

        // milliseconds
        Assert.assertEquals(invoke("datePart", source, "milliseconds"), 789);
        Assert.assertEquals(invoke("datePart", source, "ms"), 789);
        Assert.assertEquals(invoke("datePart", source, "S"), 789);

        // nanos
        Assert.assertEquals(invoke("datePart", source, "nanos"), 789000);
        Assert.assertEquals(invoke("datePart", source, "nano"), 789000);
        Assert.assertEquals(invoke("datePart", source, "n"), 789000);

        // years
        Assert.assertEquals(invoke("datePart", source, "years"), 2018);
        Assert.assertEquals(invoke("datePart", source, "year"), 2018);

        // months
        Assert.assertEquals(invoke("datePart", source, "months"), 4);
        Assert.assertEquals(invoke("datePart", source, "month"), 4);

        // weeks
        Assert.assertEquals(invoke("datePart", source, "weeks"), 5);
        Assert.assertEquals(invoke("datePart", source, "week"), 5);
        Assert.assertEquals(invoke("datePart", source, "w"), 5);

        // days, day, d
        Assert.assertEquals(invoke("datePart", source, "days"), 30);
        Assert.assertEquals(invoke("datePart", source, "day"), 30);
        Assert.assertEquals(invoke("datePart", source, "d"), 30);

        // weekday
        Assert.assertEquals(invoke("datePart", source, "weekday"), "MONDAY");

        // time
        Assert.assertEquals(invoke("datePart", source, "time"), 1525132544000l);
    }

    // Convert Date to Calendar
    private Calendar dateToCalendar(Date date) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTime(date);
        return calendar;
    }

}
