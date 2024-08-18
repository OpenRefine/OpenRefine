/*******************************************************************************
 * Copyright (C) 2024, OpenRefine contributors
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

package com.google.refine.operations.row;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.model.Row;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class RowAdditionOperationTests extends RefineTest {

    String json;

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "row-addition", RowAdditionOperation.class);
    }

    @BeforeMethod
    public void setUp() {
        json = "{"
                + "\"op\":\"core/row-addition\","
                + "\"rows\":[{\"starred\":false,\"flagged\":false,\"cells\":[]},{\"starred\":false,\"flagged\":false,\"cells\":[]}],"
                + "\"index\":0,"
                + "\"description\":\"Add rows\"}";
    }

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Test
    public void testDeserialization() throws IOException {
        RowAdditionOperation op = ParsingUtilities.mapper.readValue(json, RowAdditionOperation.class);
        TestUtils.isSerializedTo(op, json);
    }

    @Test
    public void testSerialization() throws JsonProcessingException {
        List<Row> rows = new ArrayList<>(2);
        rows.add(new Row(0)); // Blank row
        rows.add(new Row(0)); // Blank row
        int index = 0; // Prepend rows

        RowAdditionOperation op = new RowAdditionOperation(rows, index);
        ObjectMapper objectMapper = new ObjectMapper();
        String serializedObject = objectMapper.writeValueAsString(op);
        assertEquals(serializedObject, json);
    }

}
