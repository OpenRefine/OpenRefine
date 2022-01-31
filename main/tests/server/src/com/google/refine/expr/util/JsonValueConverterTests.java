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

package com.google.refine.expr.util;

import static org.testng.Assert.assertEquals;

import java.io.IOException;

import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.util.ParsingUtilities;

public class JsonValueConverterTests {

    private void fieldEquals(String json, Object expectedValue) {
        try {
            ObjectNode n = (ObjectNode) ParsingUtilities.mapper.readTree(json);
            assertEquals(expectedValue, JsonValueConverter.convert(n.get("foo")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testConvertJsonObject() throws IOException {
        fieldEquals("{\"foo\":{\"ob\":\"ject\"}}", ParsingUtilities.mapper.readTree("{\"ob\":\"ject\"}"));
    }

    @Test
    public void testConvertJsonArray() throws IOException {
        fieldEquals("{\"foo\":[1,2]}", ParsingUtilities.mapper.readTree("[1,2]"));
    }

    @Test
    public void testConvertInt() {
        fieldEquals("{\"foo\":3}", 3);
    }

    @Test
    public void testConvertFloat() {
        fieldEquals("{\"foo\":3.14}", 3.14);
    }

    @Test
    public void testConvertBool() {
        fieldEquals("{\"foo\":true}", true);
    }

    @Test
    public void testConvertNull() {
        fieldEquals("{\"foo\":null}", null);
    }

    @Test
    public void testConvertString() {
        fieldEquals("{\"foo\":\"bar\"}", "bar");
    }

    @Test
    public void testConvertNoField() {
        fieldEquals("{}", null);
    }
}
