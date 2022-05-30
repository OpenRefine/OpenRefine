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

package com.google.refine.io;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.ProjectMetadata;
import com.google.refine.util.TestUtils;

public class FileProjectManagerTests {

    protected File workspaceDir;
    protected File workspaceFile;

    @BeforeMethod
    public void createDirectory() throws IOException {
        workspaceDir = TestUtils.createTempDirectory("openrefine-test-workspace-dir");
        workspaceFile = File.createTempFile(workspaceDir.getPath(), "workspace.json");
    }

    protected class FileProjectManagerStub extends FileProjectManager {

        protected FileProjectManagerStub(File dir) {
            super(dir);
            _projectsMetadata.put(5555L, mock(ProjectMetadata.class));

        }
    }

    @Test
    public void serializeFileProjectManager() {
        FileProjectManager manager = new FileProjectManagerStub(workspaceDir);
        String json = "{\n" +
                "       \"preferences\" : {\n" +
                "         \"entries\" : {\n" +
                "           \"scripting.expressions\" : {\n" +
                "             \"class\" : \"com.google.refine.preference.TopList\",\n" +
                "             \"list\" : [ ],\n" +
                "             \"top\" : 100\n" +
                "           },\n" +
                "           \"scripting.starred-expressions\" : {\n" +
                "             \"class\" : \"com.google.refine.preference.TopList\",\n" +
                "             \"list\" : [ ],\n" +
                "             \"top\" : 2147483647\n" +
                "           }\n" +
                "         }\n" +
                "       },\n" +
                "       \"projectIDs\" : [ 5555 ]\n" +
                "     }";
        TestUtils.isSerializedTo(manager, json);

    }

    /**
     * Test that we can save and restore non-ASCII characters. For best effectiveness, this should be run with a
     * non-UTF8 default encoding for Java e.g. java -Dfile.encoding=cp1252 to simulate running on a Windows system
     */
    @Test
    public void saveReloadMultinationalCharacter() throws IOException {
        FileProjectManager manager = new FileProjectManagerStub(workspaceDir);
        manager.getPreferenceStore().put("testPref", "Refiné");
        manager.saveWorkspace();
        manager = new FileProjectManagerStub(workspaceDir);
        assertEquals(manager.getPreferenceStore().get("testPref"), "Refiné");
    }

    /**
     * Issue fix Issue #1418 Issue #3719 Issue #3277 deleting the only existing project and saving the workspace should
     * remove the projectID from workspace.json
     */
    @Test
    public void deleteProjectAndSaveWorkspace() throws IOException {
        FileProjectManager manager = new FileProjectManagerStub(workspaceDir);
        manager.saveToFile(workspaceFile);
        manager.deleteProject(5555);
        manager.saveToFile(workspaceFile);

        InputStream inputStream = new FileInputStream(workspaceFile);
        JsonObject json = JSON.parse(inputStream);
        assertTrue(json.get("projectIDs").getAsArray().isEmpty());
    }
}
