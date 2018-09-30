package com.google.refine.tests.io;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.io.FileProjectManager;
import com.google.refine.model.metadata.ProjectMetadata;
import com.google.refine.tests.util.TestUtils;

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
