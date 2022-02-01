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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeMethod;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.commands.Command;
import com.google.refine.io.FileProjectManager;
import com.google.refine.util.TestUtils;

public class ExpressionCommandTestBase {

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
    }

    public void initWorkspace(String expressionsJson, String starredExpressionsJson) {
        String starred = starredExpressionsJson == null ? "{\"class\":\"com.google.refine.preference.TopList\",\"top\":2147483647," +
                "\"list\":[]}" : starredExpressionsJson;
        String expressions = expressionsJson == null ? "{\"class\":\"com.google.refine.preference.TopList\",\"top\":100,\"list\":[]}"
                : expressionsJson;
        String jsonData = "{\"projectIDs\":[]\n" +
                ",\"preferences\":{\"entries\":{\"scripting.starred-expressions\":" + starred +
                ",\"scripting.expressions\":" + expressions + "}}}";
        initWorkspace(jsonData);
    }

    public void initWorkspace(String jsonData) {
        try {
            File workspaceDir = TestUtils.createTempDirectory("openrefine-test-workspace-dir");
            File jsonPath = new File(workspaceDir, "workspace.json");
            FileUtils.writeStringToFile(jsonPath, jsonData);
            FileProjectManager.initialize(workspaceDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void assertResponseJsonIs(String expectedJson) {
        String actualJson = writer.toString();
        if (!TestUtils.equalAsJson(expectedJson, actualJson)) {
            try {
                TestUtils.jsonDiff(expectedJson, actualJson);
            } catch (JsonParseException | JsonMappingException e) {
                e.printStackTrace();
            }
        }
        TestUtils.assertEqualsAsJson(actualJson, expectedJson);
    }

}
