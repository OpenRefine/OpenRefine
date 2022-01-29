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

package com.google.refine.expr.functions.arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.util.TestUtils;

public class InArrayTests extends RefineTest {

    static Properties bindings;
    static final List<String> listArray = Arrays.asList("v1", "v2", "v3");
    static final String stringArray[] = { "v1", "v2", "v3" };

    @Test
    public void testInArrayParameters() {
        Assert.assertTrue(invoke("inArray") instanceof EvalError);
        Assert.assertTrue(invoke("inArray", "string1") instanceof EvalError);
        Assert.assertTrue(invoke("inArray", "string1", "string2") instanceof EvalError);
        Assert.assertTrue(invoke("inArray", "string1", "string2", "string3") instanceof EvalError);
    }

    @Test
    public void testInArray() {
        Assert.assertTrue((boolean) invoke("inArray", listArray, "v1"));
        Assert.assertFalse((boolean) invoke("inArray", listArray, "v4"));
        Assert.assertTrue((boolean) invoke("inArray", stringArray, "v1"));
        Assert.assertFalse((boolean) invoke("inArray", stringArray, "v4"));
    }

    @Test
    public void testInArrayWithArrayNode() {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNode = mapper.createArrayNode();
        for (int i = 1; i < 4; i++) {
            arrayNode.add("v" + i);
        }
        Assert.assertTrue((boolean) invoke("inArray", arrayNode, "v1"));
        Assert.assertFalse((boolean) invoke("inArray", arrayNode, "v4"));
    }
}
