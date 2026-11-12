/*******************************************************************************
 * Copyright (C) 2026, OpenRefine contributors
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

import com.google.refine.grel.GrelTestBase;

public class UnicodeTypeTests extends GrelTestBase {

    @Test
    public void testAscii() {
        assertEquals((String[]) invoke("unicodeType", "aB1"),
                new String[] { "lowercase letter", "uppercase letter", "decimal digit number" });
    }

    @Test
    public void testSupplementaryCharacter() {
        // U+1F600 GRINNING FACE, a surrogate pair in UTF-16
        assertEquals((String[]) invoke("unicodeType", "😀"), new String[] { "other symbol" });
        // U+20000, CJK Unified Ideographs Extension B
        assertEquals((String[]) invoke("unicodeType", "𠀀"), new String[] { "other letter" });
    }

    @Test
    public void testMixedString() {
        assertEquals((String[]) invoke("unicodeType", "a😀b"),
                new String[] { "lowercase letter", "other symbol", "lowercase letter" });
    }

    @Test
    public void testLoneSurrogate() {
        assertEquals((String[]) invoke("unicodeType", "\uD83D"), new String[] { "surrogate" });
    }

    @Test
    public void testEmptyString() {
        assertEquals((String[]) invoke("unicodeType", ""), new String[] {});
    }

    @Test
    public void testNull() {
        assertNull(invoke("unicodeType", (Object) null));
    }

    @Test
    public void testNonString() {
        assertEquals((String[]) invoke("unicodeType", 12), new String[] { "decimal digit number", "decimal digit number" });
    }
}
