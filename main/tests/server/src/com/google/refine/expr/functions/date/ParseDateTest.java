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

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import junit.framework.TestCase;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ParseDateTest extends RefineTest {

    @Test
    public void testParseDate() {
        // string date with offset date
        Map<String, String> sampleDates = Map.of(
                "October 4th, 2021", "2021-10-04T00:00:00Z",
                "October 4 2021", "2021-10-04T00:00:00Z",
                "04/10/21", "2021-10-04T00:00:00Z",
                "Oct 4, 2021", "2021-10-04T00:00:00Z",
                "Mon Oct 4, 2021", "2021-10-04T00:00:00Z",
                "Monday, October 4, 2021", "2021-10-04T00:00:00Z"

        );

        Map<String, String> sampleDateTimes = Map.of(
                "October 4th, 2021 at 12:00:13", "2021-10-04T12:00:13Z",
                "October 4 2021 at 12:00:02", "2021-10-04T12:00:02Z",
                "04/10/21 at 12:23:00", "2021-10-04T12:23:00Z",
                "Oct 4, 2021 at 12:04:00", "2021-10-04T12:04:00Z",
                "Oct 4, 2021 12:32:09", "2021-10-04T12:32:09Z");

        sampleDates.forEach((actualDate, expectedDate) -> {

            OffsetDateTime parsedDate = (OffsetDateTime) (invoke("parseDate", actualDate));

            Assert.assertEquals(parsedDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), expectedDate);
        });

        sampleDateTimes.forEach((actualDateTime, expectedDateTime) -> {
            OffsetDateTime parsedDateTime = (OffsetDateTime) (invoke("parseDate", actualDateTime));

            Assert.assertEquals(parsedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), expectedDateTime);
        });
    }

    @Test
    public void testParseDateInvalidParams() {
        Assert.assertTrue(invoke("parseDate", "Octo 4th, 2021") instanceof EvalError);
        Assert.assertTrue(invoke("parseDate") instanceof EvalError);
        Assert.assertTrue(invoke("parseDate", "October 4th, 2021", "yyyy-MM-D") instanceof EvalError);
        Assert.assertTrue(invoke("parseDate", (Object) null) instanceof EvalError);
    }
}
