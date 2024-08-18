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

import java.util.Properties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.grel.Function;
import com.google.refine.util.ParsingUtilities;

public class LengthTests extends RefineTest {

    static Properties bindings = new Properties();

    @Test
    public void testLength() throws JsonProcessingException {
        Function f = new Length();
        assertEquals(f.call(bindings, new Object[] { Long.valueOf(123) }), 3);
        assertEquals(f.call(bindings, new Object[] { Double.valueOf(123.1) }), 5);
        assertEquals(f.call(bindings, new Object[] { "12345" }), 5);

        ArrayNode array = (ArrayNode) ParsingUtilities.mapper.readTree("[1,2,3]");
        assertEquals(f.call(bindings, new Object[] { array }), 3);

        ObjectNode dict = (ObjectNode) ParsingUtilities.mapper.readTree("{\"a\": \"b\", \"c\": \"d\"}");
        assertEquals(f.call(bindings, new Object[] { dict }), 2);
    }
}
