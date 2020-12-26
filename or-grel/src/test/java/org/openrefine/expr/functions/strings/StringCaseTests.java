/*

Copyright 2011, Thomas F. Morris
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

package org.openrefine.expr.functions.strings;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.expr.EvalError;
import org.openrefine.grel.FunctionTestBase;

/**
 * Tests for string up/low/title case functions. (A very brief start so far)
 * 
 * @author Tom Morris <tfmorris@gmail.com>
 */
public class StringCaseTests extends FunctionTestBase {

    @Test
    public void testToTitlecaseInvalidParams() {
        Assert.assertTrue(invoke("toTitlecase") instanceof EvalError);
        Assert.assertTrue(invoke("toTitlecase", "one", "two", "three") instanceof EvalError);
    }

    @Test
    public void testToTitlecase() {
        Assert.assertEquals((String) (invoke("toTitlecase", "one")), "One");
        Assert.assertEquals((String) (invoke("toTitlecase", "ONE")), "One");
        Assert.assertEquals((String) (invoke("toTitlecase", "one two three")), "One Two Three");
        Assert.assertEquals((String) (invoke("toTitlecase", "C.R. SANDIDGE WINES, INC.")), "C.R. Sandidge Wines, Inc.");
        Assert.assertEquals((String) (invoke("toTitlecase", "C.R. SANDIDGE WINES, INC.", ",. ")), "C.R. Sandidge Wines, Inc.");
        Assert.assertEquals((String) (invoke("toTitlecase", "one-two-three", "-")), "One-Two-Three");
    }

}
