/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.operations.cell;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineServlet;
import com.google.refine.RefineServletStub;
import com.google.refine.RefineTest;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.io.FileProjectManager;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class KeyValueColumnizeTests extends RefineTest {

    // dependencies
    private RefineServlet servlet;
    private Project project;
    private ProjectMetadata pm;
    private ImportingJob job;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void SetUp() throws IOException, ModelException {
        servlet = new RefineServletStub();
        File dir = TestUtils.createTempDirectory("openrefine-test-workspace-dir");
        FileProjectManager.initialize(dir);
        project = new Project();
        pm = new ProjectMetadata();
        pm.setName("KeyValueColumnize test");
        ProjectManager.singleton.registerProject(project, pm);
        OperationRegistry.registerOperation(getCoreModule(), "key-value-columnize", KeyValueColumnizeOperation.class);
        ImportingManager.initialize(servlet);
        job = ImportingManager.createJob();
    }

    @AfterMethod
    public void TearDown() {
        ImportingManager.disposeJob(job.id);
        ProjectManager.singleton.deleteProject(project.id);
        job = null;
        project = null;
        pm = null;
    }

    @Test
    public void serializeKeyValueColumnizeOperation() throws Exception {
        String json = "{\"op\":\"core/key-value-columnize\","
                + "\"description\":\"Columnize by key column key column and value column value column\","
                + "\"keyColumnName\":\"key column\","
                + "\"valueColumnName\":\"value column\","
                + "\"noteColumnName\":null}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, KeyValueColumnizeOperation.class), json);

        String jsonFull = "{\"op\":\"core/key-value-columnize\","
                + "\"description\":\"Columnize by key column key column and value column value column with note column note column\","
                + "\"keyColumnName\":\"key column\","
                + "\"valueColumnName\":\"value column\","
                + "\"noteColumnName\":\"note column\"}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(jsonFull, KeyValueColumnizeOperation.class), jsonFull);
    }

    /**
     * Test in the case where an ID is available in the first column.
     * 
     * @throws Exception
     */
    @Test
    public void testKeyValueColumnizeWithID() throws Exception {
        Project project = createProject(
                new String[] { "ID", "Cat", "Val" },
                new Serializable[][] {
                        { "1", "a", "1" },
                        { "1", "b", "3" },
                        { "2", "b", "4" },
                        { "2", "c", "5" },
                        { "3", "a", "2" },
                        { "3", "b", "5" },
                        { "3", "d", "3" }
                });

        AbstractOperation op = new KeyValueColumnizeOperation(
                "Cat", "Val", null);

        runOperation(op, project);

        Project expectedProject = createProject(
                new String[] { "ID", "a", "b", "c", "d" },
                new Serializable[][] {
                        { "1", "1", "3", null, null },
                        { "2", null, "4", "5", null },
                        { "3", "2", "5", null, "3" },
                });
        assertProjectEquals(project, expectedProject);
    }

    /**
     * Test to demonstrate the intended behaviour of the function, for issue #1214
     * https://github.com/OpenRefine/OpenRefine/issues/1214
     */

    @Test
    public void testKeyValueColumnize() throws Exception {
        Project project = createProject(
                new String[] { "Key", "Value" },
                new Serializable[][] {
                        { "merchant", "Katie" },
                        { "fruit", "apple" },
                        { "price", "1.2" },
                        { "fruit", "pear" },
                        { "price", "1.5" },
                        { "merchant", "John" },
                        { "fruit", "banana" },
                        { "price", "3.1" }
                });

        AbstractOperation op = new KeyValueColumnizeOperation(
                "Key",
                "Value",
                null);

        runOperation(op, project);

        Project expectedProject = createProject(
                new String[] { "merchant", "fruit", "price" },
                new Serializable[][] {
                        { "Katie", "apple", "1.2" },
                        { null, "pear", "1.5" },
                        { "John", "banana", "3.1" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testKeyValueColumnizeNotes() throws Exception {
        Project project = createProject(
                new String[] { "Key", "Value", "Notes" },
                new Serializable[][] {
                        { "merchant", "Katie", "ref" },
                        { "fruit", "apple", "catalogue" },
                        { "price", "1.2", "pricelist" },
                        { "merchant", "John", "knowledge" },
                        { "fruit", "banana", "survey" },
                        { "price", "3.1", "legislation" }
                });

        KeyValueColumnizeOperation operation = new KeyValueColumnizeOperation(
                "Key",
                "Value",
                "Notes");

        runOperation(operation, project);

        Project expected = createProject(
                new String[] { "merchant", "fruit", "price", "Notes : merchant", "Notes : fruit", "Notes : price" },
                new Serializable[][] {
                        { "Katie", "apple", "1.2", "ref", "catalogue", "pricelist" },
                        { "John", "banana", "3.1", "knowledge", "survey", "legislation" },
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testKeyValueColumnizeIdenticalValues() throws Exception {
        Project project = createProject(
                new String[] { "Key", "Value", "wd" },
                new Serializable[][] {
                        { "merchant", "Katie", "34" },
                        { "fruit", "apple", "34" },
                        { "price", "1.2", "34" },
                        { "merchant", "John", "56" },
                        { "fruit", "banana", "56" },
                        { "price", "3.1", "56" }
                });

        KeyValueColumnizeOperation operation = new KeyValueColumnizeOperation(
                "Key",
                "Value",
                null);

        runOperation(operation, project);

        Project expected = createProject(
                new String[] { "wd", "merchant", "fruit", "price" },
                new Serializable[][] {
                        { "34", "Katie", "apple", "1.2" },
                        { "56", "John", "banana", "3.1" }
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testCopyRowsWithNoKeys() throws Exception {
        // when a key cell is empty, if there are other columns around, we simply copy those
        Project project = createProject(
                new String[] { "Key", "Value" },
                new Serializable[][] {
                        { "merchant", "Katie" },
                        { "fruit", "apple" },
                        { "price", "1.2", },
                        { null, "John", },
                        { "fruit", "banana" },
                        { "price", "3.1", }
                });

        KeyValueColumnizeOperation operation = new KeyValueColumnizeOperation(
                "Key",
                "Value",
                null);

        runOperation(operation, project);

        Project expected = createProject(
                new String[] { "merchant", "fruit", "price" },
                new Serializable[][] {
                        { "Katie", "apple", "1.2" },
                        { null, "banana", "3.1" },
                });
        assertProjectEquals(project, expected);
    }

}
