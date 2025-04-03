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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;

import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.LanguageSpecificParser;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.Parser;

public class UserDefinedKeyerTests {

    private List<String> testStrings = List.of("hello", "new", "fantastic");

    @Mock
    private LanguageSpecificParser testParser;

    @Mock
    private Evaluable testEvaluable;

    // some sample test cases taken from KeyerTests
    @DataProvider(name = "user-defined-keyer-test-cases")
    private String[][] userDefinedKeyerTestCases() {
        return new String[][] {
                { " école ÉCole ecoLe ", "ecole" },
                { " d c b a ", "a b c d" },
                { "\tABC \t DEF ", "abc def" }, // test leading and trailing whitespace
                { "bbb\taaa", "aaa bbb" },
                // {"å","aa"}, // Requested by issue #650, but conflicts with diacritic folding
                { "æø", "aeoe" }, // Norwegian replacements from #650
                { "©ß", "css" }, // issue #409 esszet
        };
    }

    @DataProvider(name = "user-defined-keyer-expressions")
    private String[][] userDefinedKeyerExpressions() {
        return new String[][] {
                { "value + ' world'", },
                { "grel:value + ' world'", },
                { "testparser:value + world" },
        };
    }

    @BeforeMethod
    public void setupMocks() throws ParsingException {
        MockitoAnnotations.openMocks(this);
        when(testParser.parse(anyString(), eq("testparser"))).thenReturn(testEvaluable);
        when(testEvaluable.evaluate(any())).thenAnswer(AdditionalAnswers
                .returnsElementsOf(testStrings.stream().map(s -> s + " world").collect(Collectors.toList())));
    }

    @BeforeMethod
    public void registerParser() {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        MetaParser.registerLanguageParser("testparser", "Test Parser", testParser, "return value");
    }

    @AfterMethod
    public void unregisterParser() {
        MetaParser.unregisterLanguageParser("grel");
        MetaParser.unregisterLanguageParser("testparser");
    }

    @Test(dataProvider = "user-defined-keyer-test-cases")
    public void testGenericCases(String testString, String expectedString) {
        UserDefinedKeyer keyer;
        try {
            String expression = "value.fingerprint()";
            keyer = new UserDefinedKeyer(expression);
        } catch (ParsingException e) {
            throw new RuntimeException(e);
        }
        Assert.assertEquals(keyer.key(testString), expectedString,
                "User defined keying for string: " + testString + " failed");
    }

    @Test(dataProvider = "user-defined-keyer-expressions")
    public void testDifferentExpressions(String expression) {
        UserDefinedKeyer keyer;
        try {
            keyer = new UserDefinedKeyer(expression);
        } catch (ParsingException e) {
            e.printStackTrace();
            Assert.fail("Could not parse expression " + expression);
            throw new RuntimeException(e);
        }

        for (String s : testStrings) {
            String output = keyer.key(s);
            Assert.assertEquals(output, s + " world",
                    "User defined keying for string: " + s + " failed");
        }
    }
}
