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

package com.google.refine.commands.row;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import com.google.refine.util.TestUtils;

public class GetRowsCommandTest extends RefineTest {

    HttpServletRequest request = null;
    HttpServletResponse response = null;
    Command command = null;
    Project project = null;
    StringWriter writer = null;
    String sortingConfigJson = null;

    @BeforeMethod
    public void setUp() throws IOException {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        project = createProject(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { null, "c" },
                        { "d", "e" },
                        { "", "f" },
                        { "g", "h" },
                });
        command = new GetRowsCommand();
        writer = new StringWriter();
        sortingConfigJson = "{"
                + "\"criteria\":["
                + "  {"
                + "     \"column\":\"bar\","
                + "     \"valueType\":\"string\","
                + "     \"reverse\":true,"
                + "     \"blankPosition\":2,"
                + "     \"errorPosition\":1,"
                + "     \"caseSensitive\":false"
                + "}]}";
        when(request.getParameter("project")).thenReturn(String.valueOf(project.id));
        when(response.getWriter()).thenReturn(new PrintWriter(writer));
    }

    @Test
    public void testJsonOutputRowsStart() throws ServletException, IOException {
        String rowJson = "{\n" +
                "       \"filtered\" : 5,\n" +
                "       \"limit\" : 2,\n" +
                "       \"mode\" : \"row-based\",\n" +
                "       \"pool\" : {\n" +
                "         \"recons\" : { }\n" +
                "       },\n" +
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
                "       \"nextPageStart\" : 2,\n" +
                "       \"total\" : 5,\n" +
                "       \"totalRows\" : 5\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
        when(request.getParameter("start")).thenReturn("0");
        when(request.getParameter("limit")).thenReturn("2");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), rowJson);
    }

    @Test
    public void testJsonOutputRowsStartWithNoNextPage() throws ServletException, IOException {
        String rowJson = "{\n" +
                "       \"filtered\" : 5,\n" +
                "       \"limit\" : 2,\n" +
                "       \"mode\" : \"row-based\",\n" +
                "       \"pool\" : {\n" +
                "         \"recons\" : { }\n" +
                "       },\n" +
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
                "       \"nextPageStart\": 2,\n" +
                "       \"total\" : 5,\n" +
                "       \"totalRows\" : 5\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
        when(request.getParameter("start")).thenReturn("0");
        when(request.getParameter("limit")).thenReturn("2");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), rowJson);
    }

    @Test
    public void testJsonOutputRowsEnd() throws ServletException, IOException {
        String rowJson = "{\n" +
                "       \"filtered\" : 5,\n" +
                "       \"limit\" : 1,\n" +
                "       \"mode\" : \"row-based\",\n" +
                "       \"pool\" : {\n" +
                "         \"recons\" : { }\n" +
                "       },\n" +
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
                "       \"previousPageEnd\": 1,\n" +
                "       \"nextPageStart\": 2,\n" +
                "       \"total\" : 5,\n" +
                "       \"totalRows\" : 5\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
        when(request.getParameter("end")).thenReturn("2");
        when(request.getParameter("limit")).thenReturn("1");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), rowJson);
    }

    @Test
    public void testJsonOutputRowsEndWithNoPreviousPage() throws ServletException, IOException {
        String rowJson = "{\n" +
                "       \"filtered\" : 5,\n" +
                "       \"limit\" : 3,\n" +
                "       \"mode\" : \"row-based\",\n" +
                "       \"pool\" : {\n" +
                "         \"recons\" : { }\n" +
                "       },\n" +
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
                "       \"nextPageStart\": 2,\n" +
                "       \"total\" : 5,\n" +
                "       \"totalRows\" : 5\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
        when(request.getParameter("end")).thenReturn("2");
        when(request.getParameter("limit")).thenReturn("3");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), rowJson);
    }

    @Test
    public void testJsonOutputRowsSorted() throws ServletException, IOException {
        String rowJson = "{\n"
                + "       \"end\" : 2,\n"
                + "       \"filtered\" : 5,\n"
                + "       \"limit\" : 3,\n"
                + "       \"mode\" : \"row-based\",\n"
                + "       \"nextPageStart\" : 2,\n"
                + "       \"pool\" : {\n"
                + "         \"recons\" : { }\n"
                + "       },\n"
                + "       \"rows\" : [ {\n"
                + "         \"cells\" : [ {\n"
                + "           \"v\" : \"g\"\n"
                + "         }, {\n"
                + "           \"v\" : \"h\"\n"
                + "         } ],\n"
                + "         \"flagged\" : false,\n"
                + "         \"i\" : 4,\n"
                + "         \"k\" : 0,\n"
                + "         \"starred\" : false\n"
                + "       }, {\n"
                + "         \"cells\" : [ {\n"
                + "           \"v\" : \"\"\n"
                + "         }, {\n"
                + "           \"v\" : \"f\"\n"
                + "         } ],\n"
                + "         \"flagged\" : false,\n"
                + "         \"i\" : 3,\n"
                + "         \"k\" : 1,\n"
                + "         \"starred\" : false\n"
                + "       } ],\n"
                + "       \"total\" : 5,\n"
                + "       \"totalRows\" : 5\n"
                + "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
        when(request.getParameter("end")).thenReturn("2");
        when(request.getParameter("limit")).thenReturn("3");
        when(request.getParameter("sorting")).thenReturn(sortingConfigJson);
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), rowJson);
    }

    @Test
    public void testJsonOutputSingleRecord() throws ServletException, IOException {
        String recordJson = "{\n" +
                "       \"filtered\" : 3,\n" +
                "       \"limit\" : 1,\n" +
                "       \"mode\" : \"record-based\",\n" +
                "       \"pool\" : {\n" +
                "         \"recons\" : { }\n" +
                "       },\n" +
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
                "       \"nextPageStart\" : 2,\n" +
                "       \"total\" : 3,\n" +
                "       \"totalRows\": 5\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"record-based\",\"facets\":[]}");
        when(request.getParameter("start")).thenReturn("0");
        when(request.getParameter("limit")).thenReturn("1");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), recordJson);
    }

    @Test
    public void testJsonOutputTwoRecords() throws ServletException, IOException {
        String recordJson = "{\n" +
                "       \"filtered\" : 3,\n" +
                "       \"limit\" : 2,\n" +
                "       \"mode\" : \"record-based\",\n" +
                "       \"pool\" : {\n" +
                "         \"recons\" : { }\n" +
                "       },\n" +
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
                "       }, {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"d\"\n" +
                "         }, {\n" +
                "           \"v\" : \"e\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 2,\n" +
                "         \"j\" : 1,\n" +
                "         \"k\" : 2,\n" +
                "         \"starred\" : false\n" +
                "       }, {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"\"\n" +
                "         }, {\n" +
                "           \"v\" : \"f\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 3,\n" +
                "         \"k\" : 3,\n" +
                "         \"starred\" : false\n" +
                "       } ],\n" +
                "       \"start\" : 0,\n" +
                "       \"nextPageStart\" : 4,\n" +
                "       \"total\" : 3,\n" +
                "       \"totalRows\": 5\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"record-based\",\"facets\":[]}");
        when(request.getParameter("start")).thenReturn("0");
        when(request.getParameter("limit")).thenReturn("2");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), recordJson);
    }

    @Test
    public void testJsonOutputRecordStartFromOffset() throws ServletException, IOException {
        String recordJson = "{\n" +
                "       \"filtered\" : 3,\n" +
                "       \"limit\" : 1,\n" +
                "       \"mode\" : \"record-based\",\n" +
                "       \"pool\" : {\n" +
                "         \"recons\" : { }\n" +
                "       },\n" +
                "       \"rows\" : [ {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"d\"\n" +
                "         }, {\n" +
                "           \"v\" : \"e\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 2,\n" +
                "         \"j\" : 1,\n" +
                "         \"k\" : 2,\n" +
                "         \"starred\" : false\n" +
                "       }, {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"\"\n" +
                "         }, {\n" +
                "           \"v\" : \"f\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 3,\n" +
                "         \"k\" : 3,\n" +
                "         \"starred\" : false\n" +
                "       } ],\n" +
                "       \"start\" : 1,\n" +
                "       \"previousPageEnd\" : 1,\n" +
                "       \"nextPageStart\" : 4,\n" +
                "       \"total\" : 3,\n" +
                "       \"totalRows\": 5\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"record-based\",\"facets\":[]}");
        when(request.getParameter("start")).thenReturn("1");
        when(request.getParameter("limit")).thenReturn("1");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), recordJson);
    }

    @Test
    public void testJsonOutputRecordEndToOffset() throws ServletException, IOException {
        String recordJson = "{\n" +
                "       \"filtered\" : 3,\n" +
                "       \"limit\" : 1,\n" +
                "       \"mode\" : \"record-based\",\n" +
                "       \"pool\" : {\n" +
                "         \"recons\" : { }\n" +
                "       },\n" +
                "       \"rows\" : [ {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"d\"\n" +
                "         }, {\n" +
                "           \"v\" : \"e\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 2,\n" +
                "         \"j\" : 1,\n" +
                "         \"k\" : 2,\n" +
                "         \"starred\" : false\n" +
                "       }, {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"\"\n" +
                "         }, {\n" +
                "           \"v\" : \"f\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 3,\n" +
                "         \"k\" : 3,\n" +
                "         \"starred\" : false\n" +
                "       } ],\n" +
                "       \"end\" : 4,\n" +
                "       \"previousPageEnd\" : 2,\n" +
                "       \"nextPageStart\" : 4,\n" +
                "       \"total\" : 3,\n" +
                "       \"totalRows\": 5\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"record-based\",\"facets\":[]}");
        when(request.getParameter("end")).thenReturn("4");
        when(request.getParameter("limit")).thenReturn("1");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), recordJson);
    }

    @Test
    public void testJsonOutputRecordWithSorting() throws ServletException, IOException {
        String recordJson = "{\n"
                + "       \"filtered\" : 3,\n"
                + "       \"limit\" : 1,\n"
                + "       \"mode\" : \"record-based\",\n"
                + "       \"nextPageStart\" : 3,\n"
                + "       \"pool\" : {\n"
                + "         \"recons\" : { }\n"
                + "       },\n"
                + "       \"previousPageEnd\" : 1,\n"
                + "       \"rows\" : [ {\n"
                + "         \"cells\" : [ {\n"
                + "           \"v\" : \"d\"\n"
                + "         }, {\n"
                + "           \"v\" : \"e\"\n"
                + "         } ],\n"
                + "         \"flagged\" : false,\n"
                + "         \"i\" : 2,\n"
                + "         \"j\" : 1,\n"
                + "         \"k\" : 1,\n"
                + "         \"starred\" : false\n"
                + "       }, {\n"
                + "         \"cells\" : [ {\n"
                + "           \"v\" : \"\"\n"
                + "         }, {\n"
                + "           \"v\" : \"f\"\n"
                + "         } ],\n"
                + "         \"flagged\" : false,\n"
                + "         \"i\" : 3,\n"
                + "         \"k\" : 2,\n"
                + "         \"starred\" : false\n"
                + "       } ],\n"
                + "       \"start\" : 1,\n"
                + "       \"total\" : 3,\n"
                + "       \"totalRows\" : 5\n"
                + "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"record-based\",\"facets\":[]}");
        when(request.getParameter("start")).thenReturn("1");
        when(request.getParameter("limit")).thenReturn("1");
        when(request.getParameter("sorting")).thenReturn(sortingConfigJson);
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), recordJson);
    }
}
