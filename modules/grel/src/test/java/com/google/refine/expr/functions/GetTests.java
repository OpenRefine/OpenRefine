/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
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

package com.google.refine.expr.functions;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.HasFieldsListImpl;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.GrelTestBase;
import com.google.refine.util.ParsingUtilities;

public class GetTests extends GrelTestBase {

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

    @DataProvider
    public Object[][] indexedValues() throws Exception {
        return new Object[][] {
                { new Object[] { "x", "y" } },
                { Arrays.asList("x", "y") },
                { ParsingUtilities.mapper.readTree("[\"x\",\"y\"]") },
                { "xy" }
        };
    }

    @Test(dataProvider = "indexedValues")
    public void testNegativeIndexBounds(Object value) throws ParsingException {
        bindings.put("value", value);
        Assert.assertEquals(MetaParser.parse("grel:value[0]").evaluate(bindings), "x");
        Assert.assertEquals(MetaParser.parse("grel:value[1]").evaluate(bindings), "y");
        Assert.assertEquals(MetaParser.parse("grel:value[-2]").evaluate(bindings), "x");
        Assert.assertEquals(MetaParser.parse("grel:value[-1]").evaluate(bindings), "y");
        Assert.assertTrue(MetaParser.parse("grel:value[-3]").evaluate(bindings) instanceof EvalError);
        Assert.assertTrue(MetaParser.parse("grel:value[-2147483648]").evaluate(bindings) instanceof EvalError);
    }

    @Test
    public void testHasFieldsListNegativeIndexBounds() throws ParsingException {
        HasFieldsListImpl values = new HasFieldsListImpl();
        values.add(null);
        values.add(null);
        bindings.put("value", values);
        Assert.assertNull(MetaParser.parse("grel:value[-2]").evaluate(bindings));
        Assert.assertNull(MetaParser.parse("grel:value[-1]").evaluate(bindings));
        Assert.assertTrue(MetaParser.parse("grel:value[-3]").evaluate(bindings) instanceof EvalError);
    }

    @Test
    public void testEmptyValuesRejectNegativeIndex() throws Exception {
        for (Object value : new Object[] { new Object[0], Arrays.asList(),
                ParsingUtilities.mapper.readTree("[]"), "" }) {
            bindings.put("value", value);
            Assert.assertTrue(MetaParser.parse("grel:value[-1]").evaluate(bindings) instanceof EvalError);
        }
    }

    @Test
    public void testPositiveOutOfBoundsUnchanged() throws Exception {
        bindings.put("value", ParsingUtilities.mapper.readTree("[\"x\",\"y\"]"));
        Assert.assertNull(MetaParser.parse("grel:value[2]").evaluate(bindings));
        bindings.put("value", new Object[] { "x", "y" });
        Assert.assertTrue(MetaParser.parse("grel:value[2]").evaluate(bindings) instanceof EvalError);
        bindings.put("value", "xy");
        Assert.assertTrue(MetaParser.parse("grel:value[2]").evaluate(bindings) instanceof EvalError);
    }

    @Test
    public void testRangeClippingUnchanged() throws ParsingException {
        Assert.assertEquals(MetaParser.parse("grel:'xy'[-3,9]").evaluate(bindings), "xy");
        Assert.assertEquals(MetaParser.parse("grel:'xy'[0,0]").evaluate(bindings), "");
        Assert.assertEquals(MetaParser.parse("grel:'xy'[0,-1]").evaluate(bindings), "x");
    }

}
