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

package com.google.refine.operations.cell;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.RefineTest;
import com.google.refine.browsing.DecoratedValue;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.facets.ListFacet;
import com.google.refine.expr.MetaParser;
import com.google.refine.grel.Parser;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class BlankDownTests extends RefineTest {

    Project project = null;
    Project projectToBlankDown = null;
    Project projectForRecordKey = null;
    ListFacet.ListFacetConfig facet;

    @BeforeMethod
    public void registerGRELParser() {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
    }

    @AfterMethod
    public void unregisterGRELParser() {
        MetaParser.unregisterLanguageParser("grel");
    }

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "blank-down", BlankDownOperation.class);
    }

    @BeforeMethod
    public void setUp() {
        project = createProject(
                new String[] { "key", "first", "second" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { null, "d", "c" },
                        { "e", "f", "c" },
                        { null, null, "c" }
                });

        projectToBlankDown = createProject(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", "b", "d" },
                        { "e", "b", "f" },
                        { null, "g", "h" },
                        { null, "g", "i" }
                });

        projectForRecordKey = createProject(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { "a", "b" },
                        { "e", "b" },
                        { "e", "g" },
                        { "e", "g" }
                });

        facet = new ListFacet.ListFacetConfig();
        facet.name = "hello";
        facet.expression = "grel:value";
        facet.columnName = "hello";
    }

    @AfterMethod
    public void tearDown() {
        ProjectManager.singleton.deleteProject(project.id);
        ProjectManager.singleton.deleteProject(projectToBlankDown.id);
        ProjectManager.singleton.deleteProject(projectForRecordKey.id);
    }

    @Test
    public void serializeBlankDownOperation() throws Exception {
        String json = "{\"op\":\"core/blank-down\","
                + "\"description\":\"Blank down cells in column my column\","
                + "\"engineConfig\":{\"mode\":\"record-based\",\"facets\":[]},"
                + "\"columnName\":\"my column\"}";
        AbstractOperation op = ParsingUtilities.mapper.readValue(json, BlankDownOperation.class);
        TestUtils.isSerializedTo(op, json);
    }

    @Test
    public void testBlankDownRecordsNoFacets() throws Exception {
        BlankDownOperation operation = new BlankDownOperation(EngineConfig.reconstruct("{\"mode\":\"record-based\",\"facets\":[]}"), "bar");

        runOperation(operation, projectToBlankDown);

        Project expectedProject = createProject(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", null, "d" },
                        { "e", "b", "f" },
                        { null, "g", "h" },
                        { null, null, "i" }
                });

        assertProjectEquals(projectToBlankDown, expectedProject);
    }

    @Test
    public void testBlankDownRowsNoFacets() throws Exception {
        BlankDownOperation operation = new BlankDownOperation(EngineConfig.reconstruct("{\"mode\":\"row-based\",\"facets\":[]}"), "bar");

        runOperation(operation, projectToBlankDown);

        Project expectedProject = createProject(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", null, "d" },
                        { "e", null, "f" },
                        { null, "g", "h" },
                        { null, null, "i" }
                });

        assertProjectEquals(projectToBlankDown, expectedProject);
    }

    @Test
    public void testKeyColumnIndex() throws Exception {
        // Shift all column indices
        for (Row r : project.rows) {
            r.cells.add(0, null);
        }
        List<Column> newColumns = new ArrayList<>();
        for (Column c : project.columnModel.columns) {
            newColumns.add(new Column(c.getCellIndex() + 1, c.getName()));
        }
        project.columnModel.columns.clear();
        project.columnModel.columns.addAll(newColumns);
        project.columnModel.update();

        AbstractOperation op = new BlankDownOperation(
                EngineConfig.reconstruct("{\"mode\":\"record-based\",\"facets\":[]}"),
                "second");

        runOperation(op, project);

        Project expectedProject = createProject(
                new String[] { "key", "first", "second" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { null, "d", null },
                        { "e", "f", "c" },
                        { null, null, null },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testBlankDownRowsFacets() throws Exception {
        facet.selection = Arrays.asList(
                new DecoratedValue("c", "c"),
                new DecoratedValue("f", "f"),
                new DecoratedValue("i", "i"));
        EngineConfig engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RowBased);
        BlankDownOperation operation = new BlankDownOperation(engineConfig, "bar");

        runOperation(operation, projectToBlankDown);

        Project expected = createProject(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", "b", "d" },
                        { "e", null, "f" },
                        { null, "g", "h" },
                        { null, "g", "i" }
                });

        assertProjectEquals(projectToBlankDown, expected);
    }

    @Test
    public void testBlankDownRecordsFacets() throws Exception {
        facet.selection = Arrays.asList(
                new DecoratedValue("c", "c"));
        EngineConfig engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RecordBased);
        BlankDownOperation operation = new BlankDownOperation(engineConfig, "bar");

        runOperation(operation, projectToBlankDown);

        Project expected = createProject(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", null, "d" },
                        { "e", "b", "f" },
                        { null, "g", "h" },
                        { null, "g", "i" }
                });

        assertProjectEquals(projectToBlankDown, expected);
    }

    @Test
    public void testBlankDownRecordKey() throws Exception {
        BlankDownOperation operation = new BlankDownOperation(EngineConfig.reconstruct("{\"mode\":\"row-based\",\"facets\":[]}"), "foo");

        runOperation(operation, projectForRecordKey);

        Project expected = createProject(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { null, "b" },
                        { "e", "b" },
                        { null, "g" },
                        { null, "g" }
                });

        assertProjectEquals(projectForRecordKey, expected);
    }

}
