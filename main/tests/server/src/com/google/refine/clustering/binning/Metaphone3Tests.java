/*

Copyright 2010, Lawrence Philips
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

package com.google.refine.clustering.binning;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class Metaphone3Tests {

    @DataProvider(name = "metaphone3-test-cases")
    public Object[][] metaphone3TestCases() {
        return new Object[][] {
                // examples taken from the original `main` method of Metaphone3.java
                { "iron", false, "ARN", "" },
                { "witz", false, "TS", "FX" },
                { "", false, "", "" },
                { "VILLASENOR", true, "VALASANA", "VASANAR" },
                { "GUILLERMINA", true, "GARMANA", "" },
                { "PADILLA", true, "PADALA", "PADA" },
                { "BJORK", true, "BARK", "" },
                { "belle", true, "BAL", "" },
                { "ERICH", true, "ARAK", "ARAX" },
                { "CROCE", true, "KRAXA", "KRASA" },
                { "GLOWACKI", true, "GLAKA", "GLAVASKA" },
                { "qing", true, "XANG", "" },
                { "tsing", true, "XANG", "" },
        };
    }

    @Test(dataProvider = "metaphone3-test-cases")
    public void testLastNames(String source, boolean exactAndVowels, String metaph, String alternateMetaph) {
        Metaphone3 _metaphone3 = new Metaphone3();
        _metaphone3.SetEncodeExact(exactAndVowels);
        _metaphone3.SetEncodeVowels(exactAndVowels);
        _metaphone3.SetWord(source);
        _metaphone3.Encode();
        assertEquals(_metaphone3.GetMetaph(), metaph);
        assertEquals(_metaphone3.GetAlternateMetaph(), alternateMetaph);
    }
}
