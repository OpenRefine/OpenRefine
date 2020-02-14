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

package org.openrefine.grel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.expr.EvalError;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;

public class GrelTests {

    Properties bindings;

    @BeforeTest
    public void registerGREL() {
        MetaParser.registerLanguageParser("grel", "General Refine Expression Language", Parser.grelParser, "value");
    }

    @BeforeMethod
    public void SetUp() {
        bindings = ExpressionUtils.createBindings();
    }

    @AfterMethod
    public void TearDown() {
        bindings = null;
    }

    // -----------------tests------------

    @Test
    public void testInvalidSyntax() {
        String tests[] = {
                "",
                "1-1-",
                "2**3",
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
                { "1 + 1", "2" },
                { "1 + 1 + 1", "3" },
                { "1-1-1", "-1" },
                { "1-2-3", "-4" },
                { "1-(2-3)", "2" },
                { "2*3", "6" },
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
        };
        for (String[] test : tests) {
            parseEval(bindings, test);
        }
    }

    @Test
    public void testGetJsonFieldExists() throws ParsingException {
        String test[] = { "\"[{\\\"one\\\": \\\"1\\\"}]\".parseJson()[0].one", "1" };
        parseEval(bindings, test);
    }

    @Test
    public void testGetJsonFieldAbsent() throws ParsingException {
        String test = "\"[{\\\"one\\\": \\\"1\\\"}]\".parseJson()[0].two";
        Evaluable eval = MetaParser.parse("grel:" + test);
        Assert.assertNull(eval.evaluate(bindings));
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

    @Test
    public void testColumnDependencies() throws ParsingException {
        // integration test for column dependency extraction

        String baseColumn = "base";
        String tests[][] = {
                { "value", "base" },
                { "cell.recon.match.id", "base" },
                { "value + 'a'", "base" },
                { "\"constant\"", "" },
                { "1", "" },
                { "cells.foo", "foo" },
                { "value + ' ' + cells.foo.value", "base,foo" },
                { "parseHtml(value.trim())", "base" },
                { "cells", null },
                { "facetCount(value, 'value', 'col')", null },
                // this could be analyzed too, but we will never reach completeness anyway!
                // Moving to Truffle might help with partial evaluation down
                { "get(cells, 'foo'+'bar')", null },
        };
        for (String[] test : tests) {
            Evaluable eval = MetaParser.parse("grel:" + test[0]);
            Set<String> expected = test[1] == null ? null
                    : Arrays.asList(test[1].split(",")).stream()
                            .filter(s -> !s.isEmpty()).collect(Collectors.toSet());
            Assert.assertEquals(eval.getColumnDependencies(baseColumn), expected, "for expression: " + test[0]);
        }
    }

    @Test
    public void testRenameColumnDependencies() throws ParsingException {
        // integration test for column dependency extraction

        Map<String, String> substitutions = new HashMap<>();
        substitutions.put("foo", "bar");
        substitutions.put("bar", "foo");
        substitutions.put("base", "newBase");

        String tests[][] = {
                { "value", "value" },
                { "cell.recon.match.id", "cell.recon.match.id" },
                { "value + 'a'", "value + \"a\"" },
                { "\"constant\"", "\"constant\"" },
                { "1", "1" },
                { "cells.foo", "cells.bar" },
                { "cells.bar.value + ' ' + cells.foo.value", "cells.foo.value + \" \" + cells.bar.value" },
                { "parseHtml(value.trim())", "parseHtml(trim(value))" },
                { "[ cells.columnA.value, cells.foo.value ]", "[ cells.columnA.value, cells.bar.value ]" },
                { "cells", null },
                { "facetCount(value, 'value', 'col')", null },
                // this could be analyzed too, but we will never reach completeness anyway!
                // Moving to Truffle might help with partial evaluation down
                { "get(cells, 'foo'+'bar')", null },
        };
        for (String[] test : tests) {
            Evaluable eval = MetaParser.parse("grel:" + test[0]);
            Evaluable translated = eval.renameColumnDependencies(substitutions);
            String actual = translated == null ? null : translated.toString();
            Assert.assertEquals(actual, test[1], "for expression: " + test[0]);
            if (actual != null) {
                // check that the new expression can be parsed back
                Assert.assertNotNull(MetaParser.parse("grel:" + actual));
            }
        }
    }

    private void parseEval(Properties bindings, String[] test)
            throws ParsingException {
        Evaluable eval = MetaParser.parse("grel:" + test[0]);
        Object result = eval.evaluate(bindings);
        Assert.assertEquals(result.toString(), test[1],
                "Wrong result for expression: " + test[0]);
    }

}
