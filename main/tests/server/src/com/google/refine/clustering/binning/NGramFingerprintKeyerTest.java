/*
Copyright 2024 Thad M. Guidry & other contributors
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
    * Neither the names of the project or its
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

package com.google.refine.clustering.binning;

import static org.testng.Assert.assertEquals;

import org.testng.Assert;
import org.testng.annotations.Test;

public class NGramFingerprintKeyerTest {

    protected Keyer keyer = new NGramFingerprintKeyer();

    @Test
    public void testKeyWithSimpleString() {
        assertEquals(keyer.key("hello", 1), "ehlo"); // Expected result based on n-gram size 1
    }

    @Test
    public void testKeyWithPunctuation() {
        assertEquals(keyer.key("he!llo,. world", 1), "dehlorw"); // Expected result after removing punctuation
    }

    @Test
    public void testKeyWithPunctAndControlCharsAndWhitespace() {
        assertEquals(keyer.key("\u0001a, !b-c_d\u00a0d\tz\u0003", 3), "abcbcdcddddz");
        // Expected result after removing control char \u0009
        // and non-breaking space char \u00a0 (stripped during normalize method from FingerprintKeyer)
        // and should also remove control chars and punctuation
        // which then leaves the string `abcddz` to generate n-grams from
        // then building the final result fingerprint based on these size 3 n-gram's `abc`+`bcd`+`cdd`+`ddz`

    }

    private static final String[][] testNGramStrings = {
            { "abcdefg", "abbccddeeffg" },
            { "gfedcba", "bacbdcedfegf" },
            { "a b c d e f g", "abbccddeeffg" },
            { " a,b.c d\te!f?g ", "abbccddeeffg" },
            { "écÉCec", "ceec" },
            // All the whitespace characters below should be skipped
            { "a\u0009\nb\u000Bc\u000Cd\re\u0085f\u00A0g\u1680h\u2000i\u2001j\u2002k\u2003l\u2004m\u2005n\u2006o\u2007p\u2008q\u2009r\u200As\u2028t\u2029u\u202Fv\u205Fw\u3000z",
                    "abbccddeeffgghhiijjkkllmmnnooppqqrrssttuuvvwwz" },
            { "", "" }, // TODO: add more test cases
            { "", "" },
    };

    @Test
    public void testNGramKeyer() {
        keyer = new NGramFingerprintKeyer();
        for (String[] ss : testNGramStrings) {
            Assert.assertEquals(ss.length, 2, "Invalid test"); // Not a valid test
            Assert.assertEquals(keyer.key(ss[0]), ss[1],
                    "Fingerprint for string: " + ss[0] + " failed");
        }
    }

}
