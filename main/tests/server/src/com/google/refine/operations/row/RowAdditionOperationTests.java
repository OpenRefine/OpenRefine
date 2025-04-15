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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.operations.OperationDescription;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class RowAdditionOperationTests extends RefineTest {

    String legacyJson;
    String newJson;

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "row-addition", RowAdditionOperation.class);
    }

    @BeforeMethod
    public void setUp() {
        // JSON serialization before 3.9.3, which might contain some corrupted cells because of mutability issues,
        // manifesting themselves in https://github.com/OpenRefine/OpenRefine/issues/7245
        legacyJson = "{"
                + "\"op\":\"core/row-addition\","
                + "\"rows\":[{\"starred\":false,\"flagged\":false,\"cells\":[{\"foo\":3}]},{\"starred\":false,\"flagged\":false,\"cells\":[]}],"
                + "\"index\":0,"
                + "\"description\":" + new TextNode(OperationDescription.row_addition_brief()).toString() + "}";
        // JSON serialization from 3.9.3 on
        newJson = "{"
                + "\"op\":\"core/row-addition\","
                + "\"addedRows\":[{\"starred\":false,\"flagged\":false,\"cells\":[]},{\"starred\":false,\"flagged\":false,\"cells\":[]}],"
                + "\"index\":0,"
                + "\"description\":" + new TextNode(OperationDescription.row_addition_brief()).toString() + "}";
    }

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Test
    public void testLegacyDeserialization() throws IOException {
        RowAdditionOperation op = ParsingUtilities.mapper.readValue(legacyJson, RowAdditionOperation.class);
        TestUtils.isSerializedTo(op, newJson);
    }

    @Test
    public void testNewDeserialization() throws IOException {
        RowAdditionOperation op = ParsingUtilities.mapper.readValue(newJson, RowAdditionOperation.class);
        TestUtils.isSerializedTo(op, newJson);
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
        assertEquals(serializedObject, newJson);
    }


    // regression test for https://github.com/OpenRefine/OpenRefine/issues/7245
    @Test
    public void mutabilityBugRegressionTest() throws Exception {
        Project project = createProject(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b " },
                        { null, "c" },
                });

        RowAdditionOperation op = ParsingUtilities.mapper.readValue(newJson, RowAdditionOperation.class);

        // add the two rows at the beginning
        runOperation(op, project);
        // and then edit a cell in the first row
        project.rows.get(0).setCell(0, new Cell("hello", null));

        Project expected = createProject(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "hello", null },
                        { null, null },
                        { "a", "b " },
                        { null, "c" },
                });
        assertProjectEquals(project, expected);

        // the operation metadata is still unchanged
        TestUtils.isSerializedTo(op, newJson);
    }
}
