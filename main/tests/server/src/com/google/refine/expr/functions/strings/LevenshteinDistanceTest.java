/*

Copyright 2024, OpenRefine contributors
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

package com.google.refine.expr.functions.strings;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.GrelTestBase;

public class LevenshteinDistanceTest extends GrelTestBase {

    @Test
    public void testValidArguments() {
        assertEquals(invoke("levenshteinDistance", new Object[] { "New York", "NewYork" }), 1.0);
        assertEquals(invoke("levenshteinDistance", new Object[] { "M. Makeba", "Miriam Makeba" }), 5.0);
    }

    @Test
    public void testInvalidArguments() {
        assertTrue(invoke("levenshteinDistance") instanceof EvalError);
        assertTrue(invoke("levenshteinDistance", new Object[] { "test" }) instanceof EvalError);
        assertTrue(invoke("levenshteinDistance", new Object[] { "test", new Object() }) instanceof EvalError);
    }

    @Test
    public void testEmptyStrings() {
        assertEquals(invoke("levenshteinDistance", new Object[] { "", "" }), 0.0);
    }

    @Test
    public void testSingleCharacterStrings() {
        assertEquals(invoke("levenshteinDistance", new Object[] { "a", "b" }), 1.0);
    }

    @Test
    public void testDifferentLengthStrings() {
        assertEquals(invoke("levenshteinDistance", new Object[] { "", "abc" }), 3.0);
    }
}
