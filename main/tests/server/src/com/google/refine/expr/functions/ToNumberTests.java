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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Properties;

import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.Function;
import com.google.refine.util.TestUtils;

public class ToNumberTests extends RefineTest {

    private static final Double EPSILON = 0.000001;
    static Properties bindings = new Properties();

    @Test
    public void testConversions() {
        Function f = new ToNumber();
        assertEquals(f.call(bindings, new Object[] { Long.valueOf(11) }), Long.valueOf(11));
        assertEquals(f.call(bindings, new Object[] { "12" }), Long.valueOf(12));
        assertTrue((Double) f.call(bindings, new Object[] { "12345.6789" }) - Double.valueOf(12345.6789) < EPSILON);
        assertTrue(f.call(bindings, new Object[] { "abc" }) instanceof EvalError);
    }

    @Test
    public void testToNumber() {
        assertTrue(invoke("toNumber") instanceof EvalError);
        assertTrue(invoke("toNumber", (Object) null) instanceof EvalError);
        assertTrue(invoke("toNumber", "") instanceof EvalError);
        assertTrue(invoke("toNumber", "string") instanceof EvalError);
        assertEquals(invoke("toNumber", "0.0"), 0.0);
        assertEquals(invoke("toNumber", "123"), Long.valueOf(123));
        assertTrue(Math.abs((Double) invoke("toNumber", "123.456") - 123.456) < EPSILON);
        assertTrue(Math.abs((Double) invoke("toNumber", "001.234") - 1.234) < EPSILON);
        assertTrue(Math.abs((Double) invoke("toNumber", "1e2") - 100.0) < EPSILON);
        assertTrue(Math.abs((Double) invoke("toNumber", Double.parseDouble("100.0")) - 100.0) < EPSILON);
    }

}
