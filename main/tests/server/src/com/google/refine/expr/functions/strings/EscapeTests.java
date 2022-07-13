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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.util.TestUtils;

public class EscapeTests extends RefineTest {

    @Test
    public void testEscape() {
        assertNull(invoke("escape"));
        assertEquals(invoke("escape", null, "xml"), "");
        assertEquals(invoke("escape", "mystring", "html"), "mystring");
        assertEquals(invoke("escape", "mystring", "xml"), "mystring");
        assertEquals(invoke("escape", "mystring", "csv"), "mystring");
        assertEquals(invoke("escape", "mystring", "url"), "mystring");
        assertEquals(invoke("escape", "mystring", "javascript"), "mystring");
        assertEquals(invoke("escape", 1, "html"), "1");
        assertEquals(invoke("escape", 1, "xml"), "1");
        assertEquals(invoke("escape", 1, "csv"), "1");
        assertEquals(invoke("escape", 1, "url"), "1");
        assertEquals(invoke("escape", 1, "javascript"), "1");
        assertEquals(invoke("escape", Long.parseLong("1"), "html"), "1");
        assertEquals(invoke("escape", Long.parseLong("1"), "xml"), "1");
        assertEquals(invoke("escape", Long.parseLong("1"), "csv"), "1");
        assertEquals(invoke("escape", Long.parseLong("1"), "url"), "1");
        assertEquals(invoke("escape", Long.parseLong("1"), "javascript"), "1");
        assertEquals(invoke("escape", Double.parseDouble("1.23"), "html"), "1.23");
        assertEquals(invoke("escape", Double.parseDouble("1.23"), "xml"), "1.23");
        assertEquals(invoke("escape", Double.parseDouble("1.23"), "csv"), "1.23");
        assertEquals(invoke("escape", Double.parseDouble("1.23"), "url"), "1.23");
        assertEquals(invoke("escape", Double.parseDouble("1.23"), "javascript"), "1.23");

        assertEquals("\",\"", invoke("escape", ",", "csv")); // commas get quoted
        assertEquals("\"\n\"", invoke("escape", "\n", "csv")); // newlines get quoted
        assertEquals("\"\"\"\"", invoke("escape", "\"", "csv")); // quotes get doubled
    }
}
