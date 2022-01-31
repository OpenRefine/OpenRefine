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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class GetExpressionLanguageInfoCommandTests extends ExpressionCommandTestBase {

    @BeforeMethod
    public void setUp() {
        command = new GetExpressionLanguageInfoCommand();
    }

    @Test
    public void testJsonResponse() throws ServletException, IOException {

        initWorkspace(null, null);

        command.doGet(request, response);
        String jsonResponse = writer.toString();
        JsonNode result = ParsingUtilities.mapper.readValue(jsonResponse, JsonNode.class);
        TestUtils.assertEqualsAsJson(result.get("controls").get("filter").toString(),
                "{\n" +
                        "           \"description\" : \"Evaluates expression a to an array. Then for each array element, binds its value to variable name v, evaluates expression test which should return a boolean. If the boolean is true, pushes v onto the result array.\",\n"
                        +
                        "           \"params\" : \"expression a, variable v, expression test\",\n" +
                        "           \"returns\" : \"array\"\n" +
                        "         }");
    }
}
