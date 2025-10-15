/*

Copyright 2010, Google Inc.
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

package com.google.refine.clustering.knn;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
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

public class DistanceTests {

    private List<Integer> testLengths = List.of(1, 3, 5);

    @Mock
    private LanguageSpecificParser testParser;

    @Mock
    private Evaluable testEvaluable;

    private AutoCloseable mocks;

    @DataProvider(name = "user-defined-distance-expressions")
    private String[][] userDefinedKeyerExpressions() {
        return new String[][] {
                { "value1.length() - value2.length()", },
                { "grel:value1.length() - value2.length()", },
                { "testparser:value1.length() - value2.length()" },
        };
    }

    @BeforeMethod
    public void setupMocks() throws ParsingException {
        mocks = MockitoAnnotations.openMocks(this);
        when(testParser.parse(anyString(), eq("testparser"))).thenReturn(testEvaluable);
        when(testEvaluable.evaluate(any())).thenAnswer(AdditionalAnswers.returnsElementsOf(testLengths));
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
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

    @Test
    public void testUserDefinedDistance() {
        String expression = "value1.length() - value2.length()";

        SimilarityDistance distance;
        try {
            distance = new UserDefinedDistance(expression);
        } catch (ParsingException e) {
            throw new RuntimeException(e);
        }

        String[] testString = { "Ahmed", "Mohamed", "Omar", "Othman", "Aly", "Eleraky" };
        for (int i = 0; i < testString.length; i++) {
            for (int j = i + 1; j < testString.length; j++) {
                Assert.assertEquals(testString[i].length() - testString[j].length(),
                        distance.compute(testString[i], testString[j]),
                        "User defined distance for strings: " + testString[i] + ", " + testString[j] + " failed");
            }
        }
    }

    @Test(dataProvider = "user-defined-distance-expressions")
    public void testUserDefinedDistanceWithDifferentExpressions(String expression) {
        SimilarityDistance distance;
        try {
            distance = new UserDefinedDistance(expression);
        } catch (ParsingException e) {
            e.printStackTrace();
            Assert.fail("Could not parse expression " + expression);
            throw new RuntimeException(e);
        }

        for (int expectedDistance : testLengths) {
            int baseLength = 10;
            String testString1 = StringUtils.repeat("*", baseLength);
            String testString2 = StringUtils.repeat("*", baseLength - expectedDistance);
            double computedDistance = distance.compute(testString1, testString2);
            Assert.assertEquals(computedDistance, expectedDistance,
                    "User defined distance for strings: " + testString1 + ", " + testString2 + " failed");
        }
    }
}
