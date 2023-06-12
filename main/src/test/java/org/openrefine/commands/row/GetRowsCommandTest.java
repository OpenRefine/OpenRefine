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
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.openrefine.ProjectManager;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.FacetConfigResolver;
import org.openrefine.browsing.facets.ListFacet;
import org.openrefine.commands.CommandTestBase;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.history.History;
import org.openrefine.model.Cell;
import org.openrefine.model.Grid;
import org.openrefine.model.Project;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class GetRowsCommandTest extends CommandTestBase {

    Project project = null;
    Project longerProject = null;
    Project incompleteProject = null;
    EngineConfig engineConfigWithFacet = null;

    HttpServletRequest requestIncomplete = null;

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
        when(request.getParameter("project")).thenReturn(String.valueOf(project.getId()));

        Grid incompleteGrid = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { "a", "c" },
                        { Cell.PENDING_NULL, "d" }
                });

        incompleteProject = mock(Project.class);
        long incompleteProjectId = 378928L;
        when(incompleteProject.getCurrentGrid()).thenReturn(incompleteGrid);
        when(incompleteProject.getId()).thenReturn(incompleteProjectId);
        when(incompleteProject.getLastSave()).thenReturn(project.getLastSave());
        History history = mock(History.class);
        when(history.currentGridNeedsRefreshing()).thenReturn(true);
        when(history.getCurrentGrid()).thenReturn(incompleteGrid);
        when(incompleteProject.getHistory()).thenReturn(history);
        ProjectManager.singleton.registerProject(incompleteProject, ProjectManager.singleton.getProjectMetadata(project.getId()));
        requestIncomplete = mock(HttpServletRequest.class);
        when(requestIncomplete.getParameter("project")).thenReturn(String.valueOf(incompleteProjectId));

        command = new GetRowsCommand();

        FacetConfigResolver.registerFacetConfig("core", "list", ListFacet.ListFacetConfig.class);
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
    }

    @Test
    public void testNoStartOrEnd() throws ServletException, IOException {
        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
        when(request.getParameter("limit")).thenReturn("2");

        command.doPost(request, response);

        JsonNode json = ParsingUtilities.mapper.readTree(writer.toString());
        assertEquals(json.get("code").asText(), "error");
        assertEquals(json.get("message").asText(),
                "java.lang.IllegalArgumentException: Exactly one of 'start' and 'end' should be provided.");
    }

    @Test
    public void testRowsStartWithNoPreviousPage() throws ServletException, IOException {
        String rowJson = "{\n" +
                "       \"limit\" : 2,\n" +
                "       \"mode\" : \"row-based\",\n" +
                "       \"historyEntryId\": 0,\n" +
                "       \"rows\" : [ {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"a\"\n" +
                "         }, {\n" +
                "           \"v\" : \"b\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 0,\n" +
                "         \"k\" : 0,\n" +
                "         \"starred\" : false\n" +
                "       }, {\n" +
                "         \"cells\" : [ null, {\n" +
                "           \"v\" : \"c\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 1,\n" +
                "         \"k\" : 1,\n" +
                "         \"starred\" : false\n" +
                "       } ],\n" +
                "       \"start\" : 0,\n" +
                "       \"nextPageId\": 2,\n" +
                "       \"needsRefreshing\": false\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
        when(request.getParameter("start")).thenReturn("0");
        when(request.getParameter("limit")).thenReturn("2");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), rowJson);
    }

    @Test
    public void testRowsStartWithNoNextPage() throws ServletException, IOException {
        String rowJson = "{\n" +
                "       \"limit\" : 2,\n" +
                "       \"mode\" : \"row-based\",\n" +
                "       \"historyEntryId\": 0,\n" +
                "       \"rows\" : [ {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"a\"\n" +
                "         }, {\n" +
                "           \"v\" : \"b\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 0,\n" +
                "         \"k\" : 0,\n" +
                "         \"starred\" : false\n" +
                "       }, {\n" +
                "         \"cells\" : [ null, {\n" +
                "           \"v\" : \"c\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 1,\n" +
                "         \"k\" : 1,\n" +
                "         \"starred\" : false\n" +
                "       } ],\n" +
                "       \"start\" : 0,\n" +
                "       \"nextPageId\": 2,\n" +
                "       \"needsRefreshing\": false\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
        when(request.getParameter("start")).thenReturn("0");
        when(request.getParameter("limit")).thenReturn("2");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), rowJson);
    }

    @Test
    public void testRowsEnd() throws ServletException, IOException {
        String rowJson = "{\n" +
                "       \"limit\" : 1,\n" +
                "       \"mode\" : \"row-based\",\n" +
                "       \"historyEntryId\": 0,\n" +
                "       \"rows\" : [ {\n" +
                "         \"cells\" : [ null, {\n" +
                "           \"v\" : \"c\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 1,\n" +
                "         \"k\" : 1,\n" +
                "         \"starred\" : false\n" +
                "       } ],\n" +
                "       \"end\" : 2,\n" +
                "       \"previousPageId\": 1,\n" +
                "       \"nextPageId\": 2,\n" +
                "       \"needsRefreshing\": false\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
        when(request.getParameter("end")).thenReturn("2");
        when(request.getParameter("limit")).thenReturn("1");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), rowJson);
    }

    @Test
    public void testRowsEndWithNoPreviousPage() throws ServletException, IOException {
        String rowJson = "{\n" +
                "       \"limit\" : 2,\n" +
                "       \"mode\" : \"row-based\",\n" +
                "       \"historyEntryId\": 0,\n" +
                "       \"rows\" : [ {" +
                "         \"cells\": [ {\n" +
                "            \"v\" : \"a\"\n" +
                "           }, {\n" +
                "            \"v\" : \"b\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 0,\n" +
                "         \"k\" : 0,\n" +
                "         \"starred\" : false\n" +
                "       }, {\n" +
                "         \"cells\" : [ null, {\n" +
                "           \"v\" : \"c\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 1,\n" +
                "         \"k\" : 1,\n" +
                "         \"starred\" : false\n" +
                "       } ],\n" +
                "       \"end\" : 2,\n" +
                "       \"nextPageId\": 2,\n" +
                "       \"needsRefreshing\": false\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
        when(request.getParameter("end")).thenReturn("2");
        when(request.getParameter("limit")).thenReturn("2");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), rowJson);
    }

    @Test
    public void testRowsStartWithPreviousPage() throws ServletException, IOException {
        String rowJson = "{\n" +
                "       \"limit\" : 1,\n" +
                "       \"mode\" : \"row-based\",\n" +
                "       \"historyEntryId\": 0,\n" +
                "       \"rows\" : [ {\n" +
                "         \"cells\" : [ null, {\n" +
                "           \"v\" : \"c\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 1,\n" +
                "         \"k\" : 1,\n" +
                "         \"starred\" : false\n" +
                "       } ],\n" +
                "       \"start\" : 1,\n" +
                "       \"previousPageId\": 1,\n" +
                "       \"nextPageId\": 2,\n" +
                "       \"needsRefreshing\": false\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
        when(request.getParameter("start")).thenReturn("1");
        when(request.getParameter("limit")).thenReturn("1");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), rowJson);
    }

    @Test
    public void testRowsEndWithFilter() throws ServletException, IOException {
        String engineConfig = "{\"facets\":["
                + "{\"type\":\"list\",\"name\":\"foo\",\"columnName\":\"foo\",\"expression\":\"isBlank(value)\","
                + "\"omitBlank\":false,\"omitError\":false,\"selection\":[{\"v\":{\"v\":\"true\",\"l\":\"true\"}}],"
                + "\"selectBlank\":false,\"selectError\":false,\"invert\":false}"
                + "],\"mode\":\"row-based\"}";
        when(request.getParameter("engine")).thenReturn(engineConfig);
        when(request.getParameter("end")).thenReturn("2");
        when(request.getParameter("limit")).thenReturn("2");

        // there is only one row before the supplied "end" that matches the filter,
        // so we expect to also fetch some rows after, so that the full page size is returned.
        // Otherwise this means that we are going to be returning an incomplete page (which could even be empty)
        // while at the beginning of the dataset, which is confusing as a user.

        command.doPost(request, response);

        JsonNode json = ParsingUtilities.mapper.readTree(writer.toString());
        assertEquals(json.get("rows").size(), 2);
        assertEquals(json.get("limit").asInt(), 2);
        assertFalse(json.has("previousPageId"));
    }

    @Test
    public void testRowsEndWithNoNextPage() throws ServletException, IOException {
        String recordJson = "{\n" +
                "       \"limit\" : 1,\n" +
                "       \"mode\" : \"row-based\",\n" +
                "       \"historyEntryId\": 0,\n" +
                "       \"rows\" : [ {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"g\"\n" +
                "         }, {\n" +
                "           \"v\" : \"h\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 4,\n" +
                "         \"k\" : 4,\n" +
                "         \"starred\" : false\n" +
                "       }],\n" +
                "       \"end\" : 5,\n" +
                "       \"previousPageId\": 4,\n" +
                "       \"needsRefreshing\": false\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
        when(request.getParameter("end")).thenReturn("5");
        when(request.getParameter("limit")).thenReturn("1");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(recordJson, writer.toString());
    }

    @Test
    public void testRecordsStartWithoutPreviousPage() throws ServletException, IOException {
        String recordJson = "{\n" +
                "       \"limit\" : 1,\n" +
                "       \"mode\" : \"record-based\",\n" +
                "       \"historyEntryId\": 0,\n" +
                "       \"rows\" : [ {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"a\"\n" +
                "         }, {\n" +
                "           \"v\" : \"b\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 0,\n" +
                "         \"j\" : 0,\n" +
                "         \"k\" : 0,\n" +
                "         \"starred\" : false\n" +
                "       }, {\n" +
                "         \"cells\" : [ null, {\n" +
                "           \"v\" : \"c\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 1,\n" +
                "         \"k\" : 1,\n" +
                "         \"starred\" : false\n" +
                "       } ],\n" +
                "       \"start\" : 0,\n" +
                "       \"nextPageId\": 2,\n" +
                "       \"needsRefreshing\": false\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"record-based\",\"facets\":[]}");
        when(request.getParameter("start")).thenReturn("0");
        when(request.getParameter("limit")).thenReturn("1");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(recordJson, writer.toString());
    }

    @Test
    public void testRecordsStartWithPreviousPage() throws ServletException, IOException {
        String recordJson = "{\n" +
                "       \"limit\" : 1,\n" +
                "       \"mode\" : \"record-based\",\n" +
                "       \"historyEntryId\": 0,\n" +
                "       \"rows\" : [ {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"g\"\n" +
                "         }, {\n" +
                "           \"v\" : \"h\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 4,\n" +
                "         \"j\" : 4,\n" +
                "         \"k\" : 4,\n" +
                "         \"starred\" : false\n" +
                "       }],\n" +
                "       \"start\" : 4,\n" +
                "       \"previousPageId\": 4,\n" +
                "       \"needsRefreshing\": false\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"record-based\",\"facets\":[]}");
        when(request.getParameter("start")).thenReturn("4");
        when(request.getParameter("limit")).thenReturn("1");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(recordJson, writer.toString());
    }

    @Test
    public void testRecordsEndWithoutNextPage() throws ServletException, IOException {
        String recordJson = "{\n" +
                "       \"limit\" : 1,\n" +
                "       \"mode\" : \"record-based\",\n" +
                "       \"historyEntryId\": 0,\n" +
                "       \"rows\" : [ {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"g\"\n" +
                "         }, {\n" +
                "           \"v\" : \"h\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 4,\n" +
                "         \"j\" : 4,\n" +
                "         \"k\" : 4,\n" +
                "         \"starred\" : false\n" +
                "       }],\n" +
                "       \"end\" : 5,\n" +
                "       \"previousPageId\": 3,\n" +
                "       \"needsRefreshing\": false\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"record-based\",\"facets\":[]}");
        when(request.getParameter("end")).thenReturn("5");
        when(request.getParameter("limit")).thenReturn("1");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(recordJson, writer.toString());
    }

    @Test
    public void testRecordsEndWithPreviousPage() throws ServletException, IOException {
        String recordJson = "{\n" +
                "       \"limit\" : 1,\n" +
                "       \"mode\" : \"record-based\",\n" +
                "       \"historyEntryId\": 0,\n" +
                "       \"rows\" : [ {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"d\"\n" +
                "         }, {\n" +
                "           \"v\" : \"e\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 2,\n" +
                "         \"j\" : 2,\n" +
                "         \"k\" : 2,\n" +
                "         \"starred\" : false\n" +
                "       }, {\n" +
                "         \"cells\" : [ {\"v\":\"\"}, {\n" +
                "           \"v\" : \"f\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 3,\n" +
                "         \"k\" : 3,\n" +
                "         \"starred\" : false\n" +
                "       }],\n" +
                "       \"end\" : 4,\n" +
                "       \"previousPageId\": 1,\n" +
                "       \"nextPageId\": 4,\n" +
                "       \"needsRefreshing\": false\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"record-based\",\"facets\":[]}");
        when(request.getParameter("end")).thenReturn("4");
        when(request.getParameter("limit")).thenReturn("1");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(recordJson, writer.toString());
    }

    @Test
    public void testRecordsEndWithFilter() throws ServletException, IOException {
        String engineConfig = "{\"facets\":["
                + "{\"type\":\"list\",\"name\":\"foo\",\"columnName\":\"foo\",\"expression\":\"isBlank(value)\","
                + "\"omitBlank\":false,\"omitError\":false,\"selection\":[{\"v\":{\"v\":\"true\",\"l\":\"true\"}}],"
                + "\"selectBlank\":false,\"selectError\":false,\"invert\":false}"
                + "],\"mode\":\"record-based\"}";
        when(request.getParameter("engine")).thenReturn(engineConfig);
        when(request.getParameter("end")).thenReturn("1");
        when(request.getParameter("limit")).thenReturn("2");

        // same sort of scenario as in testOutputRowsWithFilteredEnd, but for records

        command.doPost(request, response);

        JsonNode json = ParsingUtilities.mapper.readTree(writer.toString());
        assertEquals(json.get("rows").size(), 4);
        assertEquals(json.get("limit").asInt(), 2);
        assertFalse(json.has("previousPageId"));
    }

    @Test
    public void testIncompleteGridNoRefreshNeeded() throws ServletException, IOException {

        when(requestIncomplete.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
        when(requestIncomplete.getParameter("start")).thenReturn("0");
        when(requestIncomplete.getParameter("limit")).thenReturn("2");

        command.doPost(requestIncomplete, response);

        String expectedJson = "{\n"
                + "  \"historyEntryId\" : 0,"
                + "  \"limit\" : 2,"
                + "  \"mode\" : \"row-based\","
                + "  \"needsRefreshing\" : false,"
                + "  \"nextPageId\" : 2,"
                + "  \"rows\" : [ {"
                + "    \"cells\" : [ {"
                + "      \"v\" : \"a\""
                + "    }, {"
                + "      \"v\" : \"b\""
                + "    } ],"
                + "    \"flagged\" : false,"
                + "    \"i\" : 0,"
                + "    \"k\" : 0,"
                + "    \"starred\" : false"
                + "  }, {"
                + "    \"cells\" : [ {\"v\":\"a\"}, {"
                + "      \"v\" : \"c\""
                + "    } ],"
                + "    \"flagged\" : false,"
                + "    \"i\" : 1,"
                + "    \"k\" : 1,"
                + "    \"starred\" : false"
                + "  } ],"
                + "  \"start\" : 0"
                + "} ";
        TestUtils.assertEqualsAsJson(expectedJson, writer.toString());
    }

    @Test
    public void testIncompleteGridWithPendingCell() throws ServletException, IOException {

        when(requestIncomplete.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
        when(requestIncomplete.getParameter("start")).thenReturn("1");
        when(requestIncomplete.getParameter("limit")).thenReturn("2");

        command.doPost(requestIncomplete, response);

        String expectedJson = "{\n"
                + "  \"historyEntryId\" : 0,"
                + "  \"limit\" : 2,"
                + "  \"mode\" : \"row-based\","
                + "  \"needsRefreshing\" : true,"
                + "  \"previousPageId\" : 1,"
                + "  \"rows\" : [ {"
                + "    \"cells\" : [ {"
                + "      \"v\" : \"a\""
                + "    }, {"
                + "      \"v\" : \"c\""
                + "    } ],"
                + "    \"flagged\" : false,"
                + "    \"i\" : 1,"
                + "    \"k\" : 1,"
                + "    \"starred\" : false"
                + "  }, {"
                + "    \"cells\" : [ {\"p\":true}, {"
                + "      \"v\" : \"d\""
                + "    } ],"
                + "    \"flagged\" : false,"
                + "    \"i\" : 2,"
                + "    \"k\" : 2,"
                + "    \"starred\" : false"
                + "  } ],"
                + "  \"start\" : 1"
                + "} ";
        TestUtils.assertEqualsAsJson(expectedJson, writer.toString());
    }

    @Test
    public void testIncompleteGridFiltered() throws ServletException, IOException {
        String engineConfig = "{\"facets\":["
                + "{\"type\":\"list\",\"name\":\"foo\",\"columnName\":\"foo\",\"expression\":\"value\","
                + "\"omitBlank\":false,\"omitError\":false,\"selection\":[{\"v\":{\"v\":\"a\",\"l\":\"a\"}}],"
                + "\"selectBlank\":false,\"selectError\":false,\"invert\":false}"
                + "],\"mode\":\"row-based\"}";

        when(requestIncomplete.getParameter("engine")).thenReturn(engineConfig);
        when(requestIncomplete.getParameter("start")).thenReturn("0");
        when(requestIncomplete.getParameter("limit")).thenReturn("3");

        command.doPost(requestIncomplete, response);

        // in this case, although the returned rows do not contain any pending cell,
        // we still need to refresh because the last row might turn out to contain an 'a'
        // after being computed, hence being in the view.

        String expectedJson = "{\n"
                + "  \"historyEntryId\" : 0,"
                + "  \"limit\" : 2,"
                + "  \"mode\" : \"row-based\","
                + "  \"needsRefreshing\" : true,"
                + "  \"rows\" : [ {"
                + "    \"cells\" : [ {"
                + "      \"v\" : \"a\""
                + "    }, {"
                + "      \"v\" : \"b\""
                + "    } ],"
                + "    \"flagged\" : false,"
                + "    \"i\" : 0,"
                + "    \"k\" : 0,"
                + "    \"starred\" : false"
                + "  }, {"
                + "    \"cells\" : [ {\"v\":\"a\"}, {"
                + "      \"v\" : \"c\""
                + "    } ],"
                + "    \"flagged\" : false,"
                + "    \"i\" : 1,"
                + "    \"k\" : 1,"
                + "    \"starred\" : false"
                + "  } ],"
                + "  \"start\" : 0"
                + "}";
        TestUtils.assertEqualsAsJson(expectedJson, writer.toString());
    }
}
