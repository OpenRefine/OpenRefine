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

    @BeforeMethod
    public void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        project = createCSVProject("a,b\nc,d\n,f");
        command = new GetRowsCommand();
        writer = new StringWriter();
        when(request.getParameter("project")).thenReturn(String.valueOf(project.id));
        try {
            when(response.getWriter()).thenReturn(new PrintWriter(writer));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testJsonOutputRows() throws ServletException, IOException {
        String rowJson = "{\n" +
                "       \"filtered\" : 2,\n" +
                "       \"limit\" : 2,\n" +
                "       \"mode\" : \"row-based\",\n" +
                "       \"pool\" : {\n" +
                "         \"recons\" : { }\n" +
                "       },\n" +
                "       \"rows\" : [ {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"c\"\n" +
                "         }, {\n" +
                "           \"v\" : \"d\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 0,\n" +
                "         \"starred\" : false\n" +
                "       }, {\n" +
                "         \"cells\" : [ null, {\n" +
                "           \"v\" : \"f\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 1,\n" +
                "         \"starred\" : false\n" +
                "       } ],\n" +
                "       \"start\" : 0,\n" +
                "       \"total\" : 2\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), rowJson);
    }

    @Test
    public void testJsonOutputRecords() throws ServletException, IOException {
        String recordJson = "{\n" +
                "       \"filtered\" : 1,\n" +
                "       \"limit\" : 2,\n" +
                "       \"mode\" : \"record-based\",\n" +
                "       \"pool\" : {\n" +
                "         \"recons\" : { }\n" +
                "       },\n" +
                "       \"rows\" : [ {\n" +
                "         \"cells\" : [ {\n" +
                "           \"v\" : \"c\"\n" +
                "         }, {\n" +
                "           \"v\" : \"d\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 0,\n" +
                "         \"j\" : 0,\n" +
                "         \"starred\" : false\n" +
                "       }, {\n" +
                "         \"cells\" : [ null, {\n" +
                "           \"v\" : \"f\"\n" +
                "         } ],\n" +
                "         \"flagged\" : false,\n" +
                "         \"i\" : 1,\n" +
                "         \"starred\" : false\n" +
                "       } ],\n" +
                "       \"start\" : 0,\n" +
                "       \"total\" : 1\n" +
                "     }";

        when(request.getParameter("engine")).thenReturn("{\"mode\":\"record-based\",\"facets\":[]}");
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), recordJson);
    }
}
