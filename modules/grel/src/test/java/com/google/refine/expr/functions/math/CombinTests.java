/*

Copyright 2025, OpenRefine contributors
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
 * Neither the name of the copyright holder nor the names of its
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

package com.google.refine.expr.functions.math;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.GrelTestBase;

public class CombinTests extends GrelTestBase {

    @Test
    public void testBaseCases() {
        Assert.assertEquals(invoke("combin", 5, 0), 1L);
        Assert.assertEquals(invoke("combin", 5, 5), 1L);
        Assert.assertEquals(invoke("combin", 0, 0), 1L);
    }

    @Test
    public void testKnownValues() {
        Assert.assertEquals(invoke("combin", 5, 2), 10L);
        Assert.assertEquals(invoke("combin", 10, 3), 120L);
        Assert.assertEquals(invoke("combin", 6, 3), 20L);
    }

    @Test
    public void testSymmetry() {
        Assert.assertEquals(invoke("combin", 10, 3), invoke("combin", 10, 7));
        Assert.assertEquals(invoke("combin", 8, 2), invoke("combin", 8, 6));
    }

    @Test
    public void testKGreaterThanN() {
        Assert.assertEquals(invoke("combin", 3, 5), 0L);
        Assert.assertEquals(invoke("combin", 5, 8), 0L);
    }

    @Test
    public void testLargeInRange() {
        Assert.assertEquals(invoke("combin", 30, 15), 155117520L);
    }

    @Test
    public void testInvalidArity() {
        Assert.assertTrue(invoke("combin") instanceof EvalError);
        Assert.assertTrue(invoke("combin", 5) instanceof EvalError);
        Assert.assertTrue(invoke("combin", 5, 2, 1) instanceof EvalError);
    }

    @Test
    public void testNonNumberArgs() {
        Assert.assertTrue(invoke("combin", "five", 2) instanceof EvalError);
        Assert.assertTrue(invoke("combin", 5, "two") instanceof EvalError);
        Assert.assertTrue(invoke("combin", null, 2) instanceof EvalError);
        Assert.assertTrue(invoke("combin", 5, null) instanceof EvalError);
    }
}
