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

import static org.testng.Assert.assertThrows;
import static org.testng.Assert.fail;

import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Project;

public class GrelTests extends GrelTestBase {

    Project project;
    Properties bindings;

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
            assertThrows("Expression failed to generate parse syntax error: " + test, ParsingException.class,
                    () -> MetaParser.parse("grel:" + test));
        }
    }

    @Test
    public void testEvalError() throws ParsingException {
        String tests[] = {
//                "1=1", // TODO: Throws NullPointerException
                "value.datePart()",
        };
        for (String test : tests) {
            Evaluable eval = MetaParser.parse("grel:" + test);
            Object result = eval.evaluate(bindings);
            Assert.assertTrue(result instanceof EvalError);
        }
    }

    static private String COMPARISON_OPERATORS[] = { "==", "!=", ">", "<", ">=", "<=", };
    static private String INVALID_OPERATORS[] = { "=<", "=<", "**", "^", "!", };
    static private String NUMERIC_OPERATORS[] = { "+", "-", "*", "/", "%", };

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
                // integer comparisons
                { "1==1", "true" },
                { "1==2", "false" },
                { "1!=2", "true" },
                { "1!=1", "false" },
//                { "1<>2", "true" },  // Scanner considers this an operator, but not the parser
                { "1>2", "false" },
                { "1<2", "true" },
                { "1>1", "false" },
                { "1>=1", "true" },
                { "1<=2", "true" },
                { "2<=2", "true" },
                { "3<=2", "false" },
                // mixed integer / float comparisons
                { "1.0==1", "true" },
                { "1.0==2", "false" },
                { "1.0>2", "false" },
                { "1.0<2", "true" },
                { "1.0>1", "false" },
                { "1.0>=1", "true" },
                { "1.0<=2", "true" },
                { "2.0<=2", "true" },
                { "3.0<=2", "false" },
                { "0/0", "NaN" },
                // TODO: The cases below currently throw an exception
//                { "1/0", "Infinity" },
//                { "-1/0", "-Infinity" },
                { "1.0/0.0", "Infinity" },
                { "-1.0/0.0", "-Infinity" },
                { "fact(4)", "24" },
                { "fact(20)", "2432902008176640000" }, // limit for Java longs
                { "fact(21)", "java.lang.ArithmeticException: Integer overflow computing factorial" },
                { "multinomial(1, 3)", "4" },
                { "multinomial(0, 4)", "1" },
                { "multinomial(18, 2)", "190" }, // limit for Java longs
                { "multinomial(18, 3)", "1330" }, // test BigInteger support
                { "multinomial(3, 5, 2)", "2520" },
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
                { "'a' + 'b'", "ab" },
                // TODO: automated fuzzing of all operators for incompatible operand types
                { "'1/1/1900'.toDate() + 1", null },
                { "'1/1/1900'.toDate() + '1/1/1800'.toDate()", null },
                { "'1/1/1900'.toDate() > '1/1/1800'.toDate()", "true" },
                { "'1/1/1900'.toDate() >= '1/1/1800'.toDate()", "true" },
                { "'1/1/1900'.toDate() < '1/1/1800'.toDate()", "false" },
                { "'1/1/1900'.toDate() <= '1/1/1800'.toDate()", "false" },
                { "'1/1/1900'.toDate() != '1/1/1800'.toDate()", "true" },
                { "'1/1/1900'.toDate() == '1/1/1800'.toDate()", "false" },
                { "'1/1/1900'.toDate() == '1/1/1900'.toDate()", "true" },
                { "'1/1/1900'.toDate() >= '1/1/1900'.toDate()", "true" },
                { "'1/1/1900'.toDate() <= '1/1/1900'.toDate()", "true" },
                { "'1/1/1900'.toDate() + ' foo'", "1900-01-01T00:00Z foo" },
                { "1 + ' foo'", "1 foo" },
                { "1.0 + ' foo'", "1.0 foo" },
                { "2 * 3.0 + ' foo'", "6.0 foo" },
                { "'a' > 'b'", "false" },
                { "'a' < 'b'", "true" },
                { "'a' == 'a'", "true" },
                { "'a' == 'b'", "false" },
                { "'a' != 'b'", "true" },
                { "'E\u0301' == 'É'", "true" }, // combining accent equivalent to single character form
//                { "", "" },
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
    public void testCrossFunctionEval() throws ParsingException {
        String test = "cross(\"Mary\", \"My Address Book\", \"friend\")";

        Evaluable eval = MetaParser.parse("grel:" + test);
        Object result = eval.evaluate(bindings);
        Assert.assertTrue(result instanceof EvalError);
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
                { "cells[\"foo\"].value+'_'+value", "base,foo" },
                { "parseHtml(value.trim())", "base" },
                { "cells", null },
                // this could be analyzed too, but we will never reach completeness anyway!
                { "get(cells, 'foo'+'bar')", null },
                // TODO this should fail to extract any dependencies because facetCount is not a pure function
                // { "facetCount(value, 'value', 'col')", null },
        };
        for (String[] test : tests) {
            Evaluable eval = MetaParser.parse("grel:" + test[0]);
            Optional<Set<String>> expected = test[1] == null ? Optional.empty()
                    : Optional.of(Arrays.asList(test[1].split(",")).stream()
                            .filter(s -> !s.isEmpty()).collect(Collectors.toSet()));
            Optional<Set<String>> columnDependencies = eval.getColumnDependencies(Optional.of(baseColumn));
            Assert.assertEquals(columnDependencies, expected, "for expression: " + test[0]);
        }
    }

    // Test for /\ throwing Internal Error
    @Test
    public void testRegex() {
        String test = "value.replace(/\\";
        try {
            MetaParser.parse("grel:" + test);
            fail("No Exception was thrown");
        } catch (ParsingException e) {
            Assert.assertEquals(e.getMessage(),
                    "Parsing error at offset 14: Missing number, string, identifier, regex, or parenthesized expression");
        }
    }
}
