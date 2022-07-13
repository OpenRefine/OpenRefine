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

package com.google.refine.commands.expr;

import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.commands.Command;
import com.google.refine.util.TestUtils;

public class ToggleStarredExpressionCommandTests extends ExpressionCommandTestBase {

    @BeforeMethod
    public void setUp() {
        command = new ToggleStarredExpressionCommand();
    }

    @Test
    public void testJsonResponse() throws ServletException, IOException {

        initWorkspace("{\n" +
                "        \"class\": \"com.google.refine.preference.TopList\",\n" +
                "        \"top\": 100,\n" +
                "        \"list\": [\n" +
                "          \"grel:facetCount(value, 'value', 'Column 1')\",\n" +
                "          \"grel:facetCount(value, 'value', 'Column 3')\",\n" +
                "          \"grel:cell.recon.match.id\"" +
                "]}",
                "{\n" +
                        "        \"class\": \"com.google.refine.preference.TopList\",\n" +
                        "        \"top\": 100,\n" +
                        "        \"list\": [\n" +
                        "          \"grel:cell.recon.match.id\"\n" +
                        "]}");

        String json = "{\n" +
                "       \"expressions\" : [ {\n" +
                "         \"code\" : \"grel:facetCount(value, 'value', 'Column 1')\"\n" +
                "       }, {\n" +
                "         \"code\" : \"grel:cell.recon.match.id\"\n" +
                "       } ]\n" +
                "     }";
        when(request.getParameter("expression")).thenReturn("grel:facetCount(value, 'value', 'Column 1')");
        when(request.getParameter("returnList")).thenReturn("yes");
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        command.doPost(request, response);
        assertResponseJsonIs(json);
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), "{\"code\":\"error\",\"message\":\"Missing or invalid csrf_token parameter\"}");
    }
}
