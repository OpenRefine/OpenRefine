/*

Copyright 2010,2011 Google Inc.
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

package org.openrefine;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.io.FileUtils;
import org.powermock.modules.testng.PowerMockTestCase;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import org.openrefine.ProjectManager;
import org.openrefine.ProjectManagerStub;
import org.openrefine.ProjectMetadata;
import org.openrefine.io.FileProjectManager;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ModelException;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.util.TestUtils;

/**
 * A base class containing various utilities to help testing Refine.
 */
public class RefineTest extends PowerMockTestCase {

    protected Logger logger;

    boolean testFailed;
    protected File workspaceDir;
    private List<Project> projects = new ArrayList<Project>();

    @BeforeSuite
    public void init() {
        System.setProperty("log4j.configuration", "tests.log4j.properties");
        try {
            workspaceDir = TestUtils.createTempDirectory("openrefine-test-workspace-dir");
            File jsonPath = new File(workspaceDir, "workspace.json");
            FileUtils.writeStringToFile(jsonPath, "{\"projectIDs\":[]\n" +
                    ",\"preferences\":{\"entries\":{\"scripting.starred-expressions\":" +
                    "{\"class\":\"org.openrefine.preference.TopList\",\"top\":2147483647," +
                    "\"list\":[]},\"scripting.expressions\":{\"class\":\"org.openrefine.preference.TopList\",\"top\":100,\"list\":[]}}}}");
            FileProjectManager.initialize(workspaceDir);

        } catch (IOException e) {
            workspaceDir = null;
            e.printStackTrace();
        }

        // This just keeps track of any failed test, for cleanupWorkspace
        testFailed = false;
    }

    @BeforeMethod
    protected void initProjectManager() {
        ProjectManager.singleton = new ProjectManagerStub();
    }

    protected Project createProjectWithColumns(String projectName, String... columnNames) throws IOException, ModelException {
        Project project = new Project();
        ProjectMetadata pm = new ProjectMetadata();
        pm.setName(projectName);
        ProjectManager.singleton.registerProject(project, pm);

        if (columnNames != null) {
            for (String columnName : columnNames) {
                int index = project.columnModel.allocateNewCellIndex();
                ColumnMetadata column = new ColumnMetadata(index, columnName);
                project.columnModel.addColumn(index, column, true);
            }
        }
        projects.add(project);
        return project;
    }

    /**
     * Helper to create a project with given contents. Not much control is given on the import options, because this
     * method is intended to be a quick way to create a project for a test. For more control over the import, just call
     * the importer directly.
     * 
     * @param columns
     *            the list of column names
     * @param rows
     *            the cell values, as a flattened array of arrays
     * @return
     */
    protected Project createProject(String[] columns, Serializable[] rows) {
        return createProject("test project", columns, rows);
    }

    /**
     * Helper to create a project with given contents. Not much control is given on the import options, because this
     * method is intended to be a quick way to create a project for a test. For more control over the import, just call
     * the importer directly.
     * 
     * The projects created via this method will be disposed of at the end of each test.
     * 
     * @param projectName
     *            the name of the project to create
     * @param columns
     *            the list of column names
     * @param rows
     *            the cell values, as a flattened array of arrays
     * @return
     */
    protected Project createProject(String projectName, String[] columns, Serializable[] rows) {
        try {
            Project project = new Project();
            ProjectMetadata meta = new ProjectMetadata();
            meta.setName(projectName);
            for (String column : columns) {
                int idx = project.columnModel.allocateNewCellIndex();
                project.columnModel.addColumn(idx, new ColumnMetadata(idx, column), false);
            }
            Row row = null;
            int idx = 0;
            for (Serializable value : rows) {
                if (idx % columns.length == 0) {
                    row = new Row(columns.length);
                    project.rows.add(row);
                }
                Cell cell = null;
                if (value != null) {
                    cell = new Cell(value, null);
                }
                row.setCell(idx % columns.length, cell);
                idx += 1;
            }
            ProjectManager.singleton.registerProject(project, meta);
            project.update();
            return project;
        } catch (ModelException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Initializes the importing options for the CSV importer.
     * 
     * @param options
     * @param sep
     * @param limit
     * @param skip
     * @param ignoreLines
     * @param headerLines
     * @param guessValueType
     * @param ignoreQuotes
     */
    public static void prepareImportOptions(ObjectNode options,
            String sep, int limit, int skip, int ignoreLines,
            int headerLines, boolean guessValueType, boolean ignoreQuotes) {

        whenGetStringOption("separator", options, sep);
        whenGetIntegerOption("limit", options, limit);
        whenGetIntegerOption("skipDataLines", options, skip);
        whenGetIntegerOption("ignoreLines", options, ignoreLines);
        whenGetIntegerOption("headerLines", options, headerLines);
        whenGetBooleanOption("guessCellValueTypes", options, guessValueType);
        whenGetBooleanOption("processQuotes", options, !ignoreQuotes);
        whenGetBooleanOption("storeBlankCellsAsNulls", options, true);
    }

    /**
     * Cleans up the projects and jobs created with createCSVProject
     */
    @AfterMethod
    protected void cleanupProjectsAndJobs() {
        for (Project project1 : ProjectManager.singleton._projects.values()) {
            if (project1 != null) {
                project1.dispose();
            }
        }

        ProjectManager.singleton._projects.clear();
        ProjectManager.singleton._projectsMetadata.clear();
    }

    /**
     * Check that a project was created with the appropriate number of columns and rows.
     * 
     * @param project
     *            project to check
     * @param numCols
     *            expected column count
     * @param numRows
     *            expected row count
     */
    public static void assertProjectCreated(Project project, int numCols, int numRows) {
        Assert.assertNotNull(project);
        Assert.assertNotNull(project.columnModel);
        Assert.assertNotNull(project.columnModel.getColumns());
        Assert.assertEquals(project.columnModel.getColumns().size(), numCols);
        Assert.assertNotNull(project.rows);
        Assert.assertEquals(project.rows.size(), numRows);
    }

    /**
     * Check that a project was created with the appropriate number of columns, rows, and records.
     * 
     * @param project
     *            project to check
     * @param numCols
     *            expected column count
     * @param numRows
     *            expected row count
     * @param numRows
     *            expected record count
     */
    public static void assertProjectCreated(Project project, int numCols, int numRows, int numRecords) {
        assertProjectCreated(project, numCols, numRows);
        Assert.assertNotNull(project.recordModel);
        Assert.assertEquals(project.recordModel.getRecordCount(), numRecords);
    }

    public void log(Project project) {
        // some quick and dirty debugging
        StringBuilder sb = new StringBuilder();
        for (ColumnMetadata c : project.columnModel.getColumns()) {
            sb.append(c.getName());
            sb.append("; ");
        }
        logger.info(sb.toString());
        for (Row r : project.rows) {
            sb = new StringBuilder();
            for (int i = 0; i < r.cells.size(); i++) {
                Cell c = r.getCell(i);
                if (c != null) {
                    sb.append(c.value);
                    sb.append("; ");
                } else {
                    sb.append("null; ");
                }
            }
            logger.info(sb.toString());
        }
    }

    // ----helpers----

    static public void whenGetBooleanOption(String name, ObjectNode options, Boolean def){
        when(options.has(name)).thenReturn(true);
        when(options.get(name)).thenReturn(def ? BooleanNode.TRUE : BooleanNode.FALSE);
    }

    static public void whenGetIntegerOption(String name, ObjectNode options, int def){
        when(options.has(name)).thenReturn(true);
        when(options.get(name)).thenReturn(new IntNode(def));
    }

    static public void whenGetStringOption(String name, ObjectNode options, String def){
        when(options.has(name)).thenReturn(true);
        when(options.get(name)).thenReturn(new TextNode(def));
    }

    static public void whenGetObjectOption(String name, ObjectNode options, ObjectNode def){
        when(options.has(name)).thenReturn(true);
        when(options.get(name)).thenReturn(def);
    }

    static public void whenGetArrayOption(String name, ObjectNode options, ArrayNode def){
        when(options.has(name)).thenReturn(true);
        when(options.get(name)).thenReturn(def);
    }

    // Works for both int, String, and JSON arrays
    static public void verifyGetArrayOption(String name, ObjectNode options) {
        verify(options, times(1)).has(name);
        verify(options, times(1)).get(name);
    }
}
