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
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.node.TextNode;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.DecoratedValue;
import com.google.refine.browsing.Engine.Mode;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.facets.ListFacet;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.MetaParser;
import com.google.refine.grel.Parser;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;
import com.google.refine.operations.OnError;
import com.google.refine.operations.OperationDescription;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class ColumnAdditionOperationTests extends RefineTest {

    protected Project project;

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
        OperationRegistry.registerOperation(getCoreModule(), "column-addition", ColumnAdditionOperation.class);
    }

    @BeforeMethod
    public void setUpInitialState() {
        project = createProject(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "v1", "a", "d" },
                        { "v3", "a", "f" },
                        { "", "a", "g" },
                        { "", "b", "h" },
                        { new EvalError("error"), "a", "i" },
                        { "v1", "b", "j" }
                });
    }

    @Test
    public void serializeColumnAdditionOperation() throws Exception {
        String description = OperationDescription.column_addition_brief("organization_json", 3, "employments",
                "grel:value.parseJson()[\"employment-summary\"].join('###'");
        String json = "{"
                + "   \"op\":\"core/column-addition\","
                + "   \"description\":" + new TextNode(description).toString() + ","
                + "   \"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
                + "   \"newColumnName\":\"organization_json\","
                + "   \"columnInsertIndex\":3,"
                + "   \"baseColumnName\":\"employments\","
                + "   \"expression\":\"grel:value.parseJson()[\\\"employment-summary\\\"].join('###')\","
                + "   \"onError\":\"set-to-blank\""
                + "}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnAdditionOperation.class), json);
    }

    @Test
    public void testValidate() {
        ColumnAdditionOperation invalidEngine = new ColumnAdditionOperation(
                invalidEngineConfig,
                "bar",
                "grel:cells[\"foo\"].value+'_'+value",
                OnError.SetToBlank,
                "newcolumn",
                2);
        assertThrows(IllegalArgumentException.class, () -> invalidEngine.validate());
        ColumnAdditionOperation missingBaseColumn = new ColumnAdditionOperation(
                EngineConfig.reconstruct("{}"),
                null,
                "grel:cells[\"foo\"].value+'_'+value",
                OnError.SetToBlank,
                "newcolumn",
                2);
        assertThrows(IllegalArgumentException.class, () -> missingBaseColumn.validate());
        ColumnAdditionOperation invalidExpression = new ColumnAdditionOperation(
                EngineConfig.reconstruct("{}"),
                "bar",
                "grel:foo(",
                OnError.SetToBlank,
                "newcolumn",
                2);
        assertThrows(IllegalArgumentException.class, () -> invalidExpression.validate());
    }

    @Test
    public void testColumnDependenciesIncludeFacets() {
        ListFacet.ListFacetConfig facetConfig = new ListFacet.ListFacetConfig();
        facetConfig.name = "my facet";
        facetConfig.expression = "grel:value";
        facetConfig.columnName = "other_column";
        facetConfig.selection = Collections.singletonList(new DecoratedValue("a", "a"));
        EngineConfig engineConfig = new EngineConfig(Collections.singletonList(facetConfig), Mode.RowBased);

        ColumnAdditionOperation operation = new ColumnAdditionOperation(
                engineConfig,
                "bar",
                "grel:cells[\"foo\"].value+'_'+value",
                OnError.SetToBlank,
                "newcolumn",
                2);

        assertEquals(operation.getColumnDependencies().get(), Set.of("foo", "bar", "other_column"));
    }

    @Test
    public void testRenameColumns() {
        String description = OperationDescription.column_addition_brief("organization_json", 3, "employments",
                "grel:value.parseJson()[\"employment-summary\"].join('###'");
        String expectedJSON = "{"
                + "   \"op\":\"core/column-addition\","
                + "   \"description\":" + new TextNode(description).toString() + ","
                + "   \"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
                + "   \"newColumnName\":\"organization_json\","
                + "   \"columnInsertIndex\":3,"
                + "   \"baseColumnName\":\"employments\","
                + "   \"expression\":\"grel:value.parseJson().get(\\\"employment-summary\\\").join('###')\","
                + "   \"onError\":\"set-to-blank\""
                + "}";

        var SUT = new ColumnAdditionOperation(
                EngineConfig.reconstruct("{}"),
                "job_titles",
                "grel:value.parseJson()[\"employment-summary\"].join('###')",
                OnError.SetToBlank,
                "new_column",
                3);

        AbstractOperation result = SUT.renameColumns(Map.of("job_titles", "employments", "new_column", "organization_json"));
        TestUtils.isSerializedTo(result, expectedJSON);
    }

    @Test
    public void testAddColumnRowsMode() throws Exception {
        ColumnAdditionOperation operation = new ColumnAdditionOperation(
                EngineConfig.deserialize("{\"mode\":\"row-based\",\"facets\":[]}"),
                "bar",
                "grel:cells[\"foo\"].value+'_'+value",
                OnError.SetToBlank,
                "newcolumn",
                2);
        assertEquals(operation.getColumnDependencies().get(), Set.of("foo", "bar"));
        assertEquals(operation.getColumnsDiff().get(), ColumnsDiff.builder().addColumn("newcolumn", "bar").build());

        runOperation(operation, project);

        Project expected = createProject(
                new String[] { "foo", "bar", "newcolumn", "hello" },
                new Serializable[][] {
                        { "v1", "a", "v1_a", "d" },
                        { "v3", "a", "v3_a", "f" },
                        { "", "a", "_a", "g" },
                        { "", "b", "_b", "h" },
                        { new EvalError("error"), "a", null, "i" },
                        { "v1", "b", "v1_b", "j" }
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testAddColumnRowsModeWithFacet() throws Exception {
        ListFacet.ListFacetConfig facetConfig = new ListFacet.ListFacetConfig();
        facetConfig.name = "my facet";
        facetConfig.expression = "grel:value";
        facetConfig.columnName = "bar";
        facetConfig.selection = Collections.singletonList(new DecoratedValue("a", "a"));
        EngineConfig engineConfig = new EngineConfig(Collections.singletonList(facetConfig), Mode.RowBased);
        ColumnAdditionOperation operation = new ColumnAdditionOperation(
                engineConfig,
                "bar",
                "grel:cells[\"foo\"].value+'_'+value",
                OnError.SetToBlank,
                "newcolumn",
                2);
        assertEquals(operation.getColumnDependencies().get(), Set.of("foo", "bar"));
        assertEquals(operation.getColumnsDiff().get(), ColumnsDiff.builder().addColumn("newcolumn", "bar").build());

        runOperation(operation, project);

        Project expected = createProject(
                new String[] { "foo", "bar", "newcolumn", "hello" },
                new Serializable[][] {
                        { "v1", "a", "v1_a", "d" },
                        { "v3", "a", "v3_a", "f" },
                        { "", "a", "_a", "g" },
                        { "", "b", null, "h" },
                        { new EvalError("error"), "a", null, "i" },
                        { "v1", "b", null, "j" }
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testAddColumnRecordsMode() throws Exception {
        ColumnAdditionOperation operation = new ColumnAdditionOperation(
                EngineConfig.deserialize("{\"mode\":\"record-based\",\"facets\":[]}"),
                "bar",
                "grel:length(row.record.cells['hello'])",
                OnError.SetToBlank,
                "newcolumn",
                2);
        assertEquals(operation.getColumnsDiff().get(), ColumnsDiff.builder().addColumn("newcolumn", "bar").build());

        runOperation(operation, project);

        Project expected = createProject(
                new String[] { "foo", "bar", "newcolumn", "hello" },
                new Serializable[][] {
                        { "v1", "a", 1, "d" },
                        { "v3", "a", 4, "f" },
                        { "", "a", 4, "g" },
                        { "", "b", 4, "h" },
                        { new EvalError("error"), "a", 4, "i" },
                        { "v1", "b", 1, "j" }
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testAddColumnRecordsModeWithFacet() throws Exception {
        ListFacet.ListFacetConfig facetConfig = new ListFacet.ListFacetConfig();
        facetConfig.name = "my facet";
        facetConfig.expression = "grel:value";
        facetConfig.columnName = "bar";
        facetConfig.selection = Collections.singletonList(new DecoratedValue("b", "b"));
        EngineConfig engineConfig = new EngineConfig(Collections.singletonList(facetConfig), Mode.RecordBased);
        ColumnAdditionOperation operation = new ColumnAdditionOperation(
                engineConfig,
                "bar",
                "grel:length(row.record.cells['hello'])",
                OnError.SetToBlank,
                "newcolumn",
                2);
        assertEquals(operation.getColumnsDiff().get(), ColumnsDiff.builder().addColumn("newcolumn", "bar").build());

        runOperation(operation, project);

        Project expected = createProject(
                new String[] { "foo", "bar", "newcolumn", "hello" },
                new Serializable[][] {
                        { "v1", "a", null, "d" },
                        { "v3", "a", 4, "f" },
                        { "", "a", 4, "g" },
                        { "", "b", 4, "h" },
                        { new EvalError("error"), "a", 4, "i" },
                        { "v1", "b", 1, "j" }
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testAddColumnRowsModeNotLocal() throws Exception {
        ColumnAdditionOperation operation = new ColumnAdditionOperation(
                new EngineConfig(Collections.emptyList(), Mode.RowBased),
                "bar",
                "grel:facetCount(value, 'value', 'bar')",
                OnError.SetToBlank,
                "newcolumn",
                2);
        assertEquals(operation.getColumnDependencies().get(), Set.of("bar"));
        assertEquals(operation.getColumnsDiff().get(), ColumnsDiff.builder().addColumn("newcolumn", "bar").build());

        runOperation(operation, project);

        Project expected = createProject(
                new String[] { "foo", "bar", "newcolumn", "hello" },
                new Serializable[][] {
                        { "v1", "a", 4, "d" },
                        { "v3", "a", 4, "f" },
                        { "", "a", 4, "g" },
                        { "", "b", 2, "h" },
                        { new EvalError("error"), "a", 4, "i" },
                        { "v1", "b", 2, "j" }
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testAddColumnRecordsModeNotLocal() throws Exception {
        ColumnAdditionOperation operation = new ColumnAdditionOperation(
                new EngineConfig(Collections.emptyList(), Mode.RecordBased),
                "bar",
                "grel:facetCount(value, 'value', 'bar')",
                OnError.SetToBlank,
                "newcolumn",
                2);
        assertEquals(operation.getColumnDependencies().get(), Set.of("bar"));
        assertEquals(operation.getColumnsDiff().get(), ColumnsDiff.builder().addColumn("newcolumn", "bar").build());

        runOperation(operation, project);

        Project expected = createProject(
                new String[] { "foo", "bar", "newcolumn", "hello" },
                new Serializable[][] {
                        { "v1", "a", 4, "d" },
                        { "v3", "a", 4, "f" },
                        { "", "a", 4, "g" },
                        { "", "b", 2, "h" },
                        { new EvalError("error"), "a", 4, "i" },
                        { "v1", "b", 2, "j" }
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testAddColumnRecordsModeNotLocalWithFacet() throws Exception {
        ListFacet.ListFacetConfig facetConfig = new ListFacet.ListFacetConfig();
        facetConfig.name = "my facet";
        facetConfig.expression = "grel:value";
        facetConfig.columnName = "bar";
        facetConfig.selection = Collections.singletonList(new DecoratedValue("b", "b"));
        EngineConfig engineConfig = new EngineConfig(Collections.singletonList(facetConfig), Mode.RecordBased);
        ColumnAdditionOperation operation = new ColumnAdditionOperation(
                engineConfig,
                "bar",
                "grel:facetCount(value, 'value', 'bar')",
                OnError.SetToBlank,
                "newcolumn",
                2);
        assertEquals(operation.getColumnDependencies().get(), Set.of("bar"));
        assertEquals(operation.getColumnsDiff().get(), ColumnsDiff.builder().addColumn("newcolumn", "bar").build());

        runOperation(operation, project);

        Project expected = createProject(
                new String[] { "foo", "bar", "newcolumn", "hello" },
                new Serializable[][] {
                        { "v1", "a", null, "d" },
                        { "v3", "a", 4, "f" },
                        { "", "a", 4, "g" },
                        { "", "b", 2, "h" },
                        { new EvalError("error"), "a", 4, "i" },
                        { "v1", "b", 2, "j" }
                });
        assertProjectEquals(project, expected);
    }
}
