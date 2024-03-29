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

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GetStarredExpressionsCommandTests extends ExpressionCommandTestBase {

    @BeforeMethod
    public void setUp() {
        command = new GetStarredExpressionsCommand();
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
                        "]}",
                "{\n" +
                        "        \"class\": \"com.google.refine.preference.TopList\",\n" +
                        "        \"top\": 100,\n" +
                        "        \"list\": [\n" +
                        "          \"grel:value\"\n" +
                        "]}");

        String json = "{\n" +
                "       \"expressions\" : [ {\n" +
                "         \"code\" : \"grel:cell.recon.match.id\"\n" +
                "       } ]\n" +
                "     }";
        command.doGet(request, response);
        assertResponseJsonIs(json);
    }

    @Test
    public void testUninitialized() throws ServletException, IOException {

        initWorkspace("{}");

        String json = "{\n" +
                "       \"expressions\" : []\n" +
                "     }";
        command.doGet(request, response);
        assertResponseJsonIs(json);
    }
}
