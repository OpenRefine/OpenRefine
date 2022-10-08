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

import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.util.CalendarParser;
import com.google.refine.expr.util.CalendarParserException;

public class ToStringTests extends RefineTest {

    @Test
    public void testToString() throws CalendarParserException {
        assertTrue(invoke("toString") instanceof EvalError);
        assertEquals(invoke("toString", (Object) null), "");
        assertEquals(invoke("toString", 100L), "100");
        assertEquals(invoke("toString", 100.0), "100.0");
        assertEquals(invoke("toString", 100.0, "%.0f"), "100");
        assertEquals(invoke("toString", 100.0, "%.1f"), String.format("%.1f", 100D));

        // test with other radix (2, 8, 10, 16)
        assertEquals(invoke("toString", 100L, "%x"), "64");
        assertEquals(invoke("toString", 100L, "%o"), "144");
        assertEquals(invoke("toString", 100L, "%d"), "100");
        assertEquals(invoke("toString", 100L, "%X"), "64");

        // test with invalid radix
        assertTrue(invoke("toString", 100L, "%z") instanceof EvalError);
        assertTrue(invoke("toString", 100L, "%") instanceof EvalError);
        assertTrue(invoke("toString", 100L, "%.") instanceof EvalError);
        assertTrue(invoke("toString", 100L, "%0") instanceof EvalError);

        // test with large number
        assertEquals(invoke("toString", 1000000000000000000L, "%d"), "1000000000000000000");

        String inputDate = "2013-06-01";
        assertEquals(invoke("toString", CalendarParser.parseAsOffsetDateTime(inputDate)), "2013-06-01T00:00:00Z");
        assertEquals(invoke("toString", CalendarParser.parseAsOffsetDateTime(inputDate), "yyyy-MM-dd"), "2013-06-01");
        assertEquals(invoke("toString", CalendarParser.parseAsOffsetDateTime(inputDate), "yyyy/dd/MM"), "2013/01/06");
        assertEquals(invoke("toString", CalendarParser.parseAsOffsetDateTime(inputDate), "yyyy-MM-dd hh:mm:ss"), "2013-06-01 12:00:00");

        String inputDateTime = "2013-06-01 13:12:11";
        assertEquals(invoke("toString", CalendarParser.parseAsOffsetDateTime(inputDateTime)), "2013-06-01T13:12:11Z");
        assertEquals(invoke("toString", CalendarParser.parseAsOffsetDateTime(inputDateTime), "yyyy-MM-dd"), "2013-06-01");
        assertEquals(invoke("toString", CalendarParser.parseAsOffsetDateTime(inputDateTime), "yyyy-MM-dd hh:mm:ss"), "2013-06-01 01:12:11");
        assertEquals(invoke("toString", CalendarParser.parseAsOffsetDateTime(inputDateTime), "yyyy-MM-dd HH:mm:ss"), "2013-06-01 13:12:11");
    }

}
