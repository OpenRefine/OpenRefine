/*
Copyright 2024 OpenRefine contributors
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

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.Parser;

public class UserDefinedKeyerTests {

    private Keyer keyer;
    // some sample test cases taken from KeyerTests
    private static final String[][] testStrings = {
            { " école ÉCole ecoLe ", "ecole" },
            { " d c b a ", "a b c d" },
            { "\tABC \t DEF ", "abc def" }, // test leading and trailing whitespace
            { "bbb\taaa", "aaa bbb" },
//        {"å","aa"}, // Requested by issue #650, but conflicts with diacritic folding
            { "æø", "aeoe" }, // Norwegian replacements from #650
            { "©ß", "css" }, // issue #409 esszet
    };

    @BeforeMethod
    public void registerGRELParser() {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
    }

    @AfterMethod
    public void unregisterGRELParser() {
        MetaParser.unregisterLanguageParser("grel");
    }

    @Test
    public void testUserDefinedKeyer1() {
        try {
            String expression = "value.fingerprint()";
            keyer = new UserDefinedKeyer(expression);
        } catch (ParsingException e) {
            throw new RuntimeException(e);
        }
        for (String[] ss : testStrings) {
            Assert.assertEquals(ss.length, 2, "Invalid test"); // Not a valid test
            Assert.assertEquals(keyer.key(ss[0]), ss[1],
                    "User defined keying for string: " + ss[0] + " failed");
        }
    }

    @Test
    public void testUserDefinedKeyer2() {
        try {
            String expression = "value + ' world'";
            keyer = new UserDefinedKeyer(expression);
        } catch (ParsingException e) {
            throw new RuntimeException(e);
        }

        String[] strs = { "hello", "new", "fantastic" };
        for (String s : strs) {
            String output = keyer.key(s);
            Assert.assertEquals(output, s + " world",
                    "User defined keying for string: " + s + " failed");
        }
    }

}
