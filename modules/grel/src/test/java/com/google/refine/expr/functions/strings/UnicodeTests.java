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

public class UnicodeTests extends GrelTestBase {

    @Test
    public void testAscii() {
        assertEquals((Integer[]) invoke("unicode", "abc"), new Integer[] { 97, 98, 99 });
    }

    @Test
    public void testSupplementaryCharacter() {
        // U+1F600 GRINNING FACE, a surrogate pair in UTF-16
        assertEquals((Integer[]) invoke("unicode", "😀"), new Integer[] { 0x1F600 });
        // U+20000, CJK Unified Ideographs Extension B
        assertEquals((Integer[]) invoke("unicode", "𠀀"), new Integer[] { 0x20000 });
    }

    @Test
    public void testMixedString() {
        assertEquals((Integer[]) invoke("unicode", "a😀b"), new Integer[] { 97, 0x1F600, 98 });
    }

    @Test
    public void testLoneSurrogate() {
        assertEquals((Integer[]) invoke("unicode", "\uD83D"), new Integer[] { 0xD83D });
    }

    @Test
    public void testEmptyString() {
        assertEquals((Integer[]) invoke("unicode", ""), new Integer[] {});
    }

    @Test
    public void testNull() {
        assertNull(invoke("unicode", (Object) null));
    }

    @Test
    public void testNonString() {
        assertEquals((Integer[]) invoke("unicode", 12), new Integer[] { 49, 50 });
    }
}
