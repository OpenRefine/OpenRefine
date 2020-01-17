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

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.io.FileProjectManager;
import com.google.refine.util.TestUtils;
import com.google.refine.ProjectMetadata;

public class FileProjectManagerTests  {
    protected File workspaceDir;
    
    @BeforeMethod
    public void createDirectory() throws IOException {
        workspaceDir = TestUtils.createTempDirectory("openrefine-test-workspace-dir");
    }
    
    protected class FileProjectManagerStub extends FileProjectManager {

        protected FileProjectManagerStub(File dir) {
            super(dir);
            _projectsMetadata.put(1234L, mock(ProjectMetadata.class));
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
                "       \"projectIDs\" : [ 1234 ]\n" + 
                "     }";
        TestUtils.isSerializedTo(manager, json);
    }
}
