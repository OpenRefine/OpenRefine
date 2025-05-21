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

package com.google.refine.operations.row;

import static org.testng.Assert.assertEquals;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.TextNode;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.DecoratedValue;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.facets.ListFacet.ListFacetConfig;
import com.google.refine.expr.MetaParser;
import com.google.refine.grel.Parser;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationDescription;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class RowFlagOperationTests extends RefineTest {

    Project project;
    ListFacetConfig facet;
    RowFlagOperation operation;

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "row-flag", RowFlagOperation.class);
    }

    @BeforeMethod
    public void registerGRELParser() {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
    }

    @AfterMethod
    public void unregisterGRELParser() {
        MetaParser.unregisterLanguageParser("grel");
    }

    @BeforeMethod
    public void createProject() {
        project = createProject(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", null, "d" },
                        { "e", null, "f" },
                        { null, "g", "h" },
                        { null, "", "i" }
                });

        facet = new ListFacetConfig();
        facet.name = "hello";
        facet.expression = "grel:value";
        facet.columnName = "hello";
        facet.selection = Arrays.asList(
                new DecoratedValue("h", "h"),
                new DecoratedValue("d", "d"));
        EngineConfig engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RowBased);
        operation = new RowFlagOperation(engineConfig, true);
    }

    @Test
    public void serializeRowFlagOperation() throws Exception {
        String json = "{"
                + "\"op\":\"core/row-flag\","
                + "\"description\":" + new TextNode(OperationDescription.row_flag_brief()).toString() + ","
                + "\"flagged\":true,"
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]}}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, RowFlagOperation.class), json);
    }

    @Test
    public void testColumnDependencies() {
        assertEquals(operation.getColumnsDiff(), Optional.of(ColumnsDiff.empty()));
        assertEquals(operation.getColumnDependencies(), Optional.of(Set.of("hello")));
    }

    @Test
    public void testRenameColumns() {
        String expectedJson = "{\n"
                + "       \"description\" : " + new TextNode(OperationDescription.row_flag_brief()).toString() + ",\n"
                + "       \"engineConfig\" : {\n"
                + "         \"facets\" : [ {\n"
                + "           \"columnName\" : \"world\",\n"
                + "           \"expression\" : \"grel:value\",\n"
                + "           \"invert\" : false,\n"
                + "           \"name\" : \"world\",\n"
                + "           \"omitBlank\" : false,\n"
                + "           \"omitError\" : false,\n"
                + "           \"selectBlank\" : false,\n"
                + "           \"selectError\" : false,\n"
                + "           \"selection\" : [ {\n"
                + "             \"v\" : {\n"
                + "               \"l\" : \"h\",\n"
                + "               \"v\" : \"h\"\n"
                + "             }\n"
                + "           }, {\n"
                + "             \"v\" : {\n"
                + "               \"l\" : \"d\",\n"
                + "               \"v\" : \"d\"\n"
                + "             }\n"
                + "           } ],\n"
                + "           \"type\" : \"list\"\n"
                + "         } ],\n"
                + "         \"mode\" : \"row-based\"\n"
                + "       },\n"
                + "       \"flagged\" : true,\n"
                + "       \"op\" : \"core/row-flag\"\n"
                + "     }";
        RowFlagOperation renamed = operation.renameColumns(Map.of("hello", "world"));

        TestUtils.isSerializedTo(renamed, expectedJson);
    }

    @Test
    public void testFlagRows() throws Exception {

        runOperation(operation, project);

        List<Boolean> flagged = project.rows.stream().map(row -> row.flagged).collect(Collectors.toList());
        Assert.assertEquals(flagged, Arrays.asList(false, true, false, true, false));
    }
}
