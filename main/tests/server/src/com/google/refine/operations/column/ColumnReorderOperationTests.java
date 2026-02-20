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

package com.google.refine.operations.column;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.TextNode;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationDescription;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class ColumnReorderOperationTests extends RefineTest {

    Project project;

    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation(getCoreModule(), "column-reorder", ColumnReorderOperation.class);
    }

    @BeforeMethod
    public void createProject() {
        project = createProject(
                new String[] { "a", "b", "c" },
                new Serializable[][] {
                        { "1|2", "d", "e" },
                        { "3", "f", "g" },
                });
    }

    @Test
    public void serializeColumnReorderOperation() {
        AbstractOperation op = new ColumnReorderOperation(Arrays.asList("b", "c", "a"), false);
        TestUtils.isSerializedTo(op, "{\"op\":\"core/column-reorder\","
                + "\"description\":" + new TextNode(OperationDescription.column_reorder_brief()).toString() + ","
                + "\"isPureReorder\": false,"
                + "\"columnNames\":[\"b\",\"c\",\"a\"]}");
    }

    @Test
    public void deserializeLegacyFormat() throws JsonMappingException, JsonProcessingException {
        ColumnReorderOperation op = ParsingUtilities.mapper.readValue("{\"op\":\"core/column-reorder\","
                + "\"columnNames\":[\"b\",\"c\",\"a\"]}", ColumnReorderOperation.class);
        assertEquals(op._columnNames, List.of("b", "c", "a"));
        assertEquals(op._isPureReorder, false);
    }

    @Test
    public void deserializeNewFormat() throws JsonMappingException, JsonProcessingException {
        String json = "{\"op\":\"core/column-reorder\","
                + "\"description\":" + new TextNode(OperationDescription.column_reorder_brief()).toString() + ","
                + "\"isPureReorder\": true,"
                + "\"columnNames\":[\"b\",\"c\",\"a\"]}";
        ColumnReorderOperation op = ParsingUtilities.mapper.readValue(json, ColumnReorderOperation.class);
        TestUtils.isSerializedTo(op, json);
    }

    @Test
    public void testValidate() {
        AbstractOperation op = new ColumnReorderOperation(null, false);
        assertThrows(IllegalArgumentException.class, () -> op.validate());
    }

    @Test
    public void testEraseCellsOnRemovedColumns() throws Exception {

        int bCol = project.columnModel.getColumnByName("b").getCellIndex();
        int cCol = project.columnModel.getColumnByName("c").getCellIndex();

        AbstractOperation op = new ColumnReorderOperation(Arrays.asList("a"), false);
        assertEquals(op.getColumnDependencies().get(), Set.of("a"));
        assertEquals(op.getColumnsDiff(), Optional.empty());

        runOperation(op, project);

        Project expectedProject = createProject(
                new String[] { "a" },
                new Serializable[][] {
                        { "1|2" },
                        { "3" },
                });
        assertProjectEquals(project, expectedProject);

        // deleted cell indices are nulled out
        Assert.assertEquals(project.rows.get(0).getCellValue(bCol), null);
        Assert.assertEquals(project.rows.get(0).getCellValue(cCol), null);
        Assert.assertEquals(project.rows.get(1).getCellValue(bCol), null);
        Assert.assertEquals(project.rows.get(1).getCellValue(cCol), null);

    }

    @Test
    public void testMixedReorder() throws Exception {
        ColumnReorderOperation SUT = new ColumnReorderOperation(Arrays.asList("c", "b"), false);
        assertEquals(SUT.getColumnDependencies().get(), Set.of("b", "c"));
        assertEquals(SUT.getColumnsDiff(), Optional.empty());

        runOperation(SUT, project);

        Project expected = createProject(
                new String[] { "c", "b" },
                new Serializable[][] {
                        { "e", "d" },
                        { "g", "f" },
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testPureReorder() throws Exception {
        ColumnReorderOperation SUT = new ColumnReorderOperation(Arrays.asList("c", "b"), true);
        assertEquals(SUT.getColumnDependencies().get(), Set.of("b", "c"));
        assertEquals(SUT.getColumnsDiff(), Optional.of(ColumnsDiff.empty()));

        runOperation(SUT, project);

        Project expected = createProject(
                new String[] { "c", "b", "a" },
                new Serializable[][] {
                        { "e", "d", "1|2" },
                        { "g", "f", "3" },
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testRename() {
        ColumnReorderOperation SUT = new ColumnReorderOperation(Arrays.asList("c", "b"), false);

        ColumnReorderOperation renamed = SUT.renameColumns(Map.of("a", "a2", "b", "b2"));

        assertEquals(renamed._columnNames, List.of("c", "b2"));
        assertEquals(renamed._isPureReorder, false);
    }

    @Test
    public void testRenamePureReorder() {
        ColumnReorderOperation SUT = new ColumnReorderOperation(Arrays.asList("c", "b"), true);

        ColumnReorderOperation renamed = SUT.renameColumns(Map.of("a", "a2", "b", "b2"));

        assertEquals(renamed._columnNames, List.of("c", "b2"));
        assertEquals(renamed._isPureReorder, true);
    }

}
