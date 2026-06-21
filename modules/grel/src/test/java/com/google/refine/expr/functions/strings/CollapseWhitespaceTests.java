/*******************************************************************************
 * Copyright (C) 2026, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.expr.functions.strings;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.GrelTestBase;

public class CollapseWhitespaceTests extends GrelTestBase {

    @Test
    public void testInvalidParams() {
        Assert.assertTrue(invoke("collapseWhitespace") instanceof EvalError);
        Assert.assertTrue(invoke("collapseWhitespace", (Object) null) instanceof EvalError);
        Assert.assertTrue(invoke("collapseWhitespace", 1) instanceof EvalError);
        Assert.assertTrue(invoke("collapseWhitespace", "one", "two") instanceof EvalError);
    }

    @Test
    public void testCollapseWhitespace() {
        Assert.assertEquals(invoke("collapseWhitespace", "  New   York  City  "), "New York City");
        Assert.assertEquals(invoke("collapseWhitespace", "\tNew\tYork\nCity\r\n"), "New York City");
        Assert.assertEquals(invoke("collapseWhitespace", "New York City"), "New York City");
        Assert.assertEquals(invoke("collapseWhitespace", ""), "");
        Assert.assertEquals(invoke("collapseWhitespace", " \t\n "), "");
    }

    @Test
    public void testCollapseUnicodeWhitespace() {
        Assert.assertEquals(invoke("collapseWhitespace", "\u00A0New\u2003York\u3000City\u00A0"), "New York City");
    }
}
