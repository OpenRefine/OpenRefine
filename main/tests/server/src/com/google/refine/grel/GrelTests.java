/*

Copyright 2011. Thomas F. Morris
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

package com.google.refine.grel;

import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Project;

public class GrelTests extends RefineTest {

    Project project;
    Properties bindings;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void SetUp() {
        project = new Project();
        bindings = ExpressionUtils.createBindings(project);
    }

    @AfterMethod
    public void TearDown() {
        project = null;
        bindings = null;
    }

    // -----------------tests------------

    @Test
    public void testInvalidSyntax() {
        String tests[] = {
                "",
                "1-1-",
                "2**3",
                "value{datePart()",
                "value}datePart()",
                "value.datePart{}",
//                "2^3" // TODO: Should this generate an error?
        };
        for (String test : tests) {
            try {
                MetaParser.parse("grel:" + test);
            } catch (ParsingException e) {
                // Test succeeded
                continue;
            }
            Assert.fail("Expression failed to generate parse syntax error: " + test);
        }
    }

    @Test
    public void testEvalError() {
        String tests[] = {
//                "1=1", // TODO: Throws NullPointerException
                "value.datePart()",
        };
        for (String test : tests) {
            try {
                Evaluable eval = MetaParser.parse("grel:" + test);
                Object result = eval.evaluate(bindings);
                Assert.assertTrue(result instanceof EvalError);
            } catch (ParsingException e) {
                Assert.fail("Unexpected parse failure: " + test);
            }
        }
    }

    @Test
    public void testMath() throws ParsingException {
        String tests[][] = {
                { "1", "1" },
                { "-1", "-1" },
                { "-1 * 2", "-2" },
                { "1 + 1", "2" },
                { "1 + 1 + 1", "3" },
                { "1-1-1", "-1" },
                { "1-2-3", "-4" },
                { "1-(2-3)", "2" },
                { "2*3", "6" },
                { "2.0*3", "6.0" },
                { "-2.0*3", "-6.0" },
                { "3%2", "1" },
                { "3/2", "1" },
                { "3.0/2", "1.5" },
                { "1==1", "true" },
                { "1==2", "false" },
                { "1>2", "false" },
                { "1<2", "true" },
                { "1>1", "false" },
                { "1>=1", "true" },
                { "1<=2", "true" },
                { "2<=2", "true" },
                { "3<=2", "false" },
                { "0/0", "NaN" },
//                { "", "" }, 
        };
        for (String[] test : tests) {
            parseEval(bindings, test);
        }
    }

    @Test
    public void testPI() throws ParsingException {
        String test[] = { "PI", "3.141592653589793" };
        parseEval(bindings, test);
    }

    @Test
    public void testString() throws ParsingException {
        String tests[][] = {
                { "1", "1" },
                { "1 + 1", "2" },
                { "1 + 1 + 1", "3" },
                { "1-1-1", "-1" },
                { "1-2-3", "-4" },
                { "1-(2-3)", "2" },
                { "2*3", "6" },
                { "3%2", "1" },
                { "3/2", "1" },
                { "3.0/2", "1.5" },
                { "1", "1" },
                { "0/0", "NaN" },
        };
        for (String[] test : tests) {
            parseEval(bindings, test);
        }
    }

    @Test
    public void testJoinJsonArray() throws ParsingException {
        String test[] = { "\"{\\\"values\\\":[\\\"one\\\",\\\"two\\\",\\\"three\\\"]}\".parseJson().values.join(\",\")", "one,two,three" };
        parseEval(bindings, test);
    }

    @Test
    public void testGetFieldFromNull() throws ParsingException {
        String test = "null.value";
        Evaluable eval = MetaParser.parse("grel:" + test);
        Assert.assertNull(eval.evaluate(bindings));
    }

    // to demonstrate bug fixing for #1204
    @Test
    public void testCrossFunctionEval() {
        String test = "cross(\"Mary\", \"My Address Book\", \"friend\")";

        try {
            Evaluable eval = MetaParser.parse("grel:" + test);
            Object result = eval.evaluate(bindings);
            Assert.assertTrue(result instanceof EvalError);
        } catch (ParsingException e) {
            Assert.fail("Unexpected parse failure for cross function: " + test);
        }
    }

}
