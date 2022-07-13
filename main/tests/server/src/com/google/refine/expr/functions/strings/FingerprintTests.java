/*
Copyright 2013 Thomas F. Morris & other contributors
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

package com.google.refine.expr.functions.strings;

import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.util.TestUtils;

public class FingerprintTests extends RefineTest {

    static Properties bindings;

    private static final String[][] testStrings = {
            { "schön", "schon" },
//        {"Ære Øre Åre", "are aere ore"},
//        {"Straße","strasse"},
            { "\tABC \t DEF ", "abc def" }, // test leading and trailing whitespace
            { "bbb\taaa", "aaa bbb" },
            { "müller", "muller" },
//      {"müller","mueller"}, // another possible interpretation
//        {"ﬁﬂĳ","fiflij"},
//        {"ﭏ","אל"},
//        {"œ ӕ","ae oe"},
            { "", "" },
    };

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Test
    public void testInvalidParams() {
        Assert.assertNull(invoke("fingerprint"));
        Assert.assertNull(invoke("fingerprint", "one", "two", "three"));
        Assert.assertNull(invoke("fingerprint", Long.getLong("1")));
    }

    @Test
    public void testNormalize() {
        for (String[] ss : testStrings) {
            Assert.assertEquals(ss.length, 2, "Invalid test"); // Not a valid test
            Assert.assertEquals((String) (invoke("fingerprint", ss[0])), ss[1],
                    "Fingerprint for string: " + ss[0] + " failed");
        }
    }

}
