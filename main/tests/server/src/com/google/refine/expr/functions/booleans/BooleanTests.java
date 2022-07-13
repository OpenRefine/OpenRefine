/*

Copyright 2013, Thomas F. Morris
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

package com.google.refine.expr.functions.booleans;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;

public class BooleanTests extends RefineTest {

    private static String TRUTH_TABLE[][] = {
            { "and", "true", "true", "true", "true" },
            { "and", "false", "false", "false", "false" },
            { "and", "true", "false", "false", "false" },
            { "and", "false", "true", "true", "false" },

            { "or", "true", "true", "true", "true" },
            { "or", "false", "false", "false", "false" },
            { "or", "true", "false", "false", "true" },
            { "or", "false", "true", "true", "true" },

            { "xor", "true", "true", "true", "false" },
            { "xor", "false", "false", "false", "false" },
            { "xor", "true", "false", "false", "true" },
            { "xor", "false", "true", "false", "true" },
    };

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Test
    public void testInvalidParams() {
        for (String op : new String[] { "and", "or", "xor" }) {
            Assert.assertTrue(invoke(op) instanceof EvalError);
            Assert.assertTrue(invoke(op, Boolean.TRUE, Integer.valueOf(1)) instanceof EvalError);
            Assert.assertTrue(invoke(op, Integer.valueOf(1), Boolean.TRUE) instanceof EvalError);
            Assert.assertTrue(invoke(op, Boolean.TRUE, "foo") instanceof EvalError);
            Assert.assertTrue(invoke(op, "foo", Boolean.TRUE) instanceof EvalError);
            Assert.assertTrue(invoke(op, Boolean.TRUE) instanceof EvalError);
        }
        String op = "not";
        Assert.assertTrue(invoke(op) instanceof EvalError);
        Assert.assertTrue(invoke(op, Boolean.TRUE, Boolean.TRUE) instanceof EvalError);
        Assert.assertTrue(invoke(op, Integer.valueOf(1)) instanceof EvalError);
        Assert.assertTrue(invoke(op, "foo") instanceof EvalError);
    }

    @Test
    public void testBinary() {
        for (String[] test : TRUTH_TABLE) {
            String operator = test[0];
            Boolean op1 = Boolean.valueOf(test[1]);
            Boolean op2 = Boolean.valueOf(test[2]);
            Boolean op3 = Boolean.valueOf(test[3]);
            Boolean result = Boolean.valueOf(test[4]);
            Assert.assertEquals(invoke(operator, op1, op2, op3), result);
        }
        Assert.assertEquals(invoke("not", Boolean.TRUE), Boolean.FALSE);
        Assert.assertEquals(invoke("not", Boolean.FALSE), Boolean.TRUE);
    }
}
