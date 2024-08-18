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

import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.commons.io.FileUtils;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.commands.CommandTestBase;
import com.google.refine.io.FileProjectManager;
import com.google.refine.model.Project;
import com.google.refine.model.ProjectStub;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.preference.TopList;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class ExpressionCommandTestBase extends CommandTestBase {

    protected static long PROJECT_ID = 1234L;

    public void initWorkspace(String globalExpressionsJson, String starredExpressionsJson, String localExpressionsJson) {
        String starred = starredExpressionsJson == null
                ? "{\"class\":\"com.google.refine.preference.TopList\",\"top\":" + Integer.MAX_VALUE + "," +
                        "\"list\":[]}"
                : starredExpressionsJson;
        String globalExpressions = globalExpressionsJson == null
                ? "{\"class\":\"com.google.refine.preference.TopList\",\"top\":100,\"list\":[]}"
                : globalExpressionsJson;
        String localExpressions = localExpressionsJson == null
                ? "{\"class\":\"com.google.refine.preference.TopList\",\"top\":100,\"list\":[]}"
                : localExpressionsJson;

        initWorkspaceInternal(starred, globalExpressions, localExpressions);
    }

    private void initWorkspaceInternal(String starred, String global, String local) {
        String jsonData = "{\"projectIDs\":[]\n" +
                ",\"preferences\":{\"entries\":{\"scripting.starred-expressions\":" + starred +
                ",\"scripting.expressions\":" + global + "}}}";
        initWorkspace(jsonData);

        Project project = new ProjectStub(PROJECT_ID);
        ProjectMetadata pm = new ProjectMetadata();
        PreferenceStore prefs = pm.getPreferenceStore();
        try {
            prefs.put("scripting.expressions", ParsingUtilities.mapper.readValue(local, TopList.class));
        } catch (JsonProcessingException e) {
            fail("Can't parse expression history JSON");
        }
        ProjectManager.singleton.registerProject(project, pm);
    }

    public void initWorkspace(String jsonData) {
        try {
            File workspaceDir = TestUtils.createTempDirectory("openrefine-test-workspace-dir");
            File jsonPath = new File(workspaceDir, "workspace.json");
            FileUtils.writeStringToFile(jsonPath, jsonData);
            FileProjectManager.initialize(workspaceDir);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
