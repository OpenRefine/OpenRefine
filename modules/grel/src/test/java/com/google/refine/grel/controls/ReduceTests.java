/*

Copyright 2026, OpenRefine contributors
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

package com.google.refine.grel.controls;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.ControlEvalError;
import com.google.refine.grel.GrelTestBase;

public class ReduceTests extends GrelTestBase {

    // an example of evaluating grel code within a test
    private Object getResult(String expression) throws ParsingException {
        Evaluable eval = MetaParser.parse("grel:" + expression);
        return eval.evaluate(bindings);
    }

    @Test
    public void testBaseReduce() throws ParsingException {
        Object result = getResult("reduce([1, 2, 3], v, acc, 0, v + acc)");
        Assert.assertEquals(result, 6L);
    }

    @Test
    public void testExpressionExceptions() throws ParsingException {
        Object result = getResult("reduce([1, 2, 3], v, acc, \"a\", v - acc)");
        Assert.assertTrue(ExpressionUtils.isError(result));

        result = getResult("reduce(1, v, acc, 0, v + acc)");
        Assert.assertTrue(ExpressionUtils.isError(result));
        String message = ((EvalError) result).message;
        Assert.assertEquals(message, ControlEvalError.expects_first_arg_array("reduce"));
    }

    @Test
    public void testArgumentLengths() {
        try {
            MetaParser.parse("grel:reduce(v, acc, \"a\", v - acc)");
        } catch (ParsingException p) {
            Assert.assertTrue(p.getMessage().contains(ControlEvalError.expects_five_args("reduce")));
        }

        try {
            MetaParser.parse("grel:reduce([1, 2, 3], v, acc, 0, v - acc, 0)");
        } catch (ParsingException p) {
            Assert.assertTrue(p.getMessage().contains(ControlEvalError.expects_five_args("reduce")));
        }
    }

    @Test
    public void testArgumentTypes() throws ParsingException {
        try {
            MetaParser.parse("grel:reduce([1, 2, 3], 0, acc, 0, v - acc)");
        } catch (ParsingException p) {
            Assert.assertTrue(p.getMessage().contains(ControlEvalError.expects_second_arg_index_var_name("reduce")));
        }

        try {
            MetaParser.parse("grel:reduce([1, 2, 3], v, 1, 0, v - acc)");
        } catch (ParsingException p) {
            Assert.assertTrue(p.getMessage().contains(ControlEvalError.expects_third_arg_element_var_name("reduce")));
        }
    }
}
