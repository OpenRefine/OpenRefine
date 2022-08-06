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

package org.openrefine.commands.row;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.FacetConfigResolver;
import org.openrefine.browsing.facets.ListFacet;
import org.openrefine.commands.CommandTestBase;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.model.Project;
import org.openrefine.util.TestUtils;

public class GetRowsCommandTest extends CommandTestBase {

    Project project = null;
    Project longerProject = null;
    EngineConfig engineConfigWithFacet = null;

    @BeforeMethod
    public void setUp() {
        project = createProject(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { null, "c" },
                        { "d", "e" },
                        { "", "f" },
                        { "g", "h" }
                });
        command = new GetRowsCommand();

        when(request.getParameter("project")).thenReturn(String.valueOf(project.getId()));
        FacetConfigResolver.registerFacetConfig("core", "list", ListFacet.ListFacetConfig.class);
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
    }

    @Test
    public void testJsonOutputRows() throws ServletException, IOException {
        String rowJson = "{\n" +
                "       \"filtered\" : 5,\n" +
                "       \"limit\" : 2,\n" +
                "       \"mode\" : \"row-based\",\n" +
                "       \"rows\" : [ {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"a\"\n" +
                "         }, {\n" +
                "           \"v\" : \"b\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 0,\n" +
                "         \"starred\" : false\n" +
                "       }, {\n" +
                "         \"cells\" : [ null, {\n" +
                "           \"v\" : \"c\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 1,\n" +
                "         \"starred\" : false\n" +
                "       } ],\n" +
                "       \"start\" : 0,\n" +
                "       \"total\" : 5,\n" +
                "       \"processed\": 5\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
        when(request.getParameter("limit")).thenReturn("2");
        command.doPost(request, response);
        TestUtils.assertEqualAsJson(rowJson, writer.toString());
    }

    @Test
    public void testAggregationLimitRowsNoFacet() throws ServletException, IOException {
        String rowJson = "{\n" +
                "       \"filtered\" : 5,\n" +
                "       \"limit\" : 1,\n" +
                "       \"mode\" : \"row-based\",\n" +
                "       \"rows\" : [ {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"a\"\n" +
                "         }, {\n" +
                "           \"v\" : \"b\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 0,\n" +
                "         \"starred\" : false\n" +
                "       } ],\n" +
                "       \"start\" : 0,\n" +
                "       \"total\" : 5,\n" +
                "       \"processed\": 5\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[],\"aggregationLimit\":2}");
        when(request.getParameter("limit")).thenReturn("1");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), rowJson);
    }

    @Test
    public void testAggregationLimitRowsFacet() throws ServletException, IOException {
        String engineConfig = "{\"facets\":["
                + "{\"type\":\"list\",\"name\":\"foo\",\"columnName\":\"foo\",\"expression\":\"isBlank(value)\","
                + "\"omitBlank\":false,\"omitError\":false,\"selection\":[{\"v\":{\"v\":\"false\",\"l\":\"false\"}}],"
                + "\"selectBlank\":false,\"selectError\":false,\"invert\":false}"
                + "],\"mode\":\"row-based\","
                + "\"aggregationLimit\":2}";

        String rowJson = "{\n" +
                "       \"filtered\" : 1,\n" +
                "       \"limit\" : 1,\n" +
                "       \"mode\" : \"row-based\",\n" +
                "       \"rows\" : [ {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"a\"\n" +
                "         }, {\n" +
                "           \"v\" : \"b\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 0,\n" +
                "         \"starred\" : false\n" +
                "       } ],\n" +
                "       \"start\" : 0,\n" +
                "       \"total\" : 5,\n" +
                "       \"processed\": 2\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn(engineConfig);
        when(request.getParameter("limit")).thenReturn("1");

        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), rowJson);
    }

    @Test
    public void testJsonOutputRecords() throws ServletException, IOException {
        String recordJson = "{\n" +
                "       \"filtered\" : 3,\n" +
                "       \"limit\" : 1,\n" +
                "       \"mode\" : \"record-based\",\n" +
                "       \"rows\" : [ {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"a\"\n" +
                "         }, {\n" +
                "           \"v\" : \"b\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 0,\n" +
                "         \"j\" : 0,\n" +
                "         \"starred\" : false\n" +
                "       }, {\n" +
                "         \"cells\" : [ null, {\n" +
                "           \"v\" : \"c\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 1,\n" +
                "         \"starred\" : false\n" +
                "       } ],\n" +
                "       \"start\" : 0,\n" +
                "       \"total\" : 3,\n" +
                "       \"processed\": 3\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"record-based\",\"facets\":[]}");
        when(request.getParameter("limit")).thenReturn("1");
        command.doPost(request, response);
        TestUtils.assertEqualAsJson(recordJson, writer.toString());
    }

    @Test
    public void testAggregationLimitRecordsNoFacet() throws ServletException, IOException {
        String recordJson = "{\n" +
                "       \"filtered\" : 3,\n" +
                "       \"limit\" : 1,\n" +
                "       \"mode\" : \"record-based\",\n" +
                "       \"rows\" : [ {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"a\"\n" +
                "         }, {\n" +
                "           \"v\" : \"b\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 0,\n" +
                "         \"j\" : 0,\n" +
                "         \"starred\" : false\n" +
                "       }, {\n" +
                "         \"cells\" : [ null, {\n" +
                "           \"v\" : \"c\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 1,\n" +
                "         \"starred\" : false\n" +
                "       } ],\n" +
                "       \"start\" : 0,\n" +
                "       \"total\" : 3,\n" +
                "       \"processed\": 3\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"record-based\",\"facets\":[],\"aggregationLimit\":2}");
        when(request.getParameter("limit")).thenReturn("1");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), recordJson);
    }

    @Test
    public void testAggregationLimitRecordsFacet() throws ServletException, IOException {
        String engineConfig = "{\"facets\":["
                + "{\"type\":\"list\",\"name\":\"foo\",\"columnName\":\"foo\",\"expression\":\"isBlank(value)\","
                + "\"omitBlank\":false,\"omitError\":false,\"selection\":[{\"v\":{\"v\":\"false\",\"l\":\"false\"}}],"
                + "\"selectBlank\":false,\"selectError\":false,\"invert\":false}"
                + "],\"mode\":\"record-based\","
                + "\"aggregationLimit\":2}";

        String recordJson = "{\n" +
                "       \"filtered\" : 2,\n" +
                "       \"limit\" : 1,\n" +
                "       \"mode\" : \"record-based\",\n" +
                "       \"rows\" : [ {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"a\"\n" +
                "         }, {\n" +
                "           \"v\" : \"b\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 0,\n" +
                "         \"j\" : 0,\n" +
                "         \"starred\" : false\n" +
                "       }, {\n" +
                "         \"cells\" : [ null, {\n" +
                "           \"v\" : \"c\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 1,\n" +
                "         \"starred\" : false\n" +
                "       } ],\n" +
                "       \"start\" : 0,\n" +
                "       \"total\" : 3,\n" +
                "       \"processed\": 2\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn(engineConfig);
        when(request.getParameter("limit")).thenReturn("1");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), recordJson);
    }
}
