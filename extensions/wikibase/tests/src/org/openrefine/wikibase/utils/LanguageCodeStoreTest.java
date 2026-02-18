/*******************************************************************************
 * Copyright (c) 2026, OpenRefine contributors
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

package org.openrefine.wikibase.utils;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.Test;

public class LanguageCodeStoreTest {

    @Test
    public void testNullEndpointUsesDefaultLists() {
        Set<String> term = LanguageCodeStore.getLanguageCodes(null, LanguageCodeStore.LanguageCodeContext.TERM);
        Set<String> monolingual =
                LanguageCodeStore.getLanguageCodes(null, LanguageCodeStore.LanguageCodeContext.MONOLINGUALTEXT);
        assertFalse(term.isEmpty());
        assertFalse(monolingual.isEmpty());
        assertTrue(term.contains("en"));
        assertTrue(monolingual.contains("en"));
    }

    @Test
    public void testTermAndMonolingualTextDiffer() {
        Set<String> term = LanguageCodeStore.getLanguageCodes(null, LanguageCodeStore.LanguageCodeContext.TERM);
        Set<String> monolingual =
                LanguageCodeStore.getLanguageCodes(null, LanguageCodeStore.LanguageCodeContext.MONOLINGUALTEXT);
        assertTrue(monolingual.size() > term.size(),
                "Monolingual text should allow more language codes than term context");
    }

    @Test
    public void testBackwardCompatibleGetLanguageCodesWithEndpointOnly() {
        Set<String> codes = LanguageCodeStore.getLanguageCodes(null);
        assertTrue(codes.contains("en"));
        assertFalse(codes.isEmpty());
    }
}
