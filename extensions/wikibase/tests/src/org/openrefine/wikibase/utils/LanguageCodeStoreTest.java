/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2018 Antonin Delpeuch
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
