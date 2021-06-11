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

package org.openrefine.commands.expr;

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

import org.openrefine.RefineTest;
import org.openrefine.commands.Command;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.model.Project;
import org.openrefine.util.TestUtils;

public class PreviewExpressionCommandTests extends RefineTest {

    protected Project project = null;
    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
    protected Command command = null;
    protected StringWriter writer = null;

    @BeforeMethod
    public void setUpRequestResponse() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        writer = new StringWriter();
        try {
            when(response.getWriter()).thenReturn(new PrintWriter(writer));
        } catch (IOException e) {
            e.printStackTrace();
        }
        command = new PreviewExpressionCommand();
        project = createProject(new String[] { "a", "b" },
                new Serializable[][] {
                        { "c", "d" },
                        { "e", "f" },
                        { "g", "h" }
                });

        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
    }

    @Test
    public void testJsonResponse() throws ServletException, IOException {

        when(request.getParameter("project")).thenReturn(Long.toString(project.getId()));
        when(request.getParameter("cellIndex")).thenReturn("1");
        when(request.getParameter("limit")).thenReturn("2");
        when(request.getParameter("expression")).thenReturn("grel:value + \"_u\"");
        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");

        String json = "{\n" + 
                "       \"code\" : \"ok\",\n" + 
                "       \"results\" : [ {"
                + "          \"rowIndex\": 0,"
                + "          \"value\": \"d\","
                + "          \"result\":\"d_u\""
                + "		},"
                + "	    {"
                + "          \"rowIndex\": 1,"
                + "          \"value\": \"f\","
                + "          \"result\": \"f_u\""
                + "     }]\n" + 
                "     }";
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), json);
    }

    @Test
    public void testParseError() throws ServletException, IOException {

        when(request.getParameter("project")).thenReturn(Long.toString(project.getId()));
        when(request.getParameter("cellIndex")).thenReturn("1");
        when(request.getParameter("limit")).thenReturn("2");
        when(request.getParameter("expression")).thenReturn("grel:value +");
        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");

        String json = "{\n" + 
                "       \"code\" : \"error\",\n" + 
                "       \"message\" : \"Parsing error at offset 7: Expecting something more at end of expression\",\n" + 
                "       \"type\" : \"parser\"\n" + 
                "     }";
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), json);
    }
}
