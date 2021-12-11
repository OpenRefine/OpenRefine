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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.powermock.modules.testng.PowerMockTestCase;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import org.openrefine.expr.Evaluable;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.io.FileProjectManager;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.model.TestingDatamodelRunner;
import org.openrefine.model.changes.LazyCachedGridStore;
import org.openrefine.model.changes.LazyChangeDataStore;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.util.TestUtils;

/**
 * A base class containing various utilities to help testing Refine.
 */
public class RefineTest extends PowerMockTestCase {

    protected static Properties bindings = null;

    protected Logger logger;

    boolean testFailed;
    protected File workspaceDir;

    private DatamodelRunner runner;

    /**
     * Method that subclasses can override to change the datamodel runner used in the test.
     */
    protected DatamodelRunner createDatamodelRunner() {
        return new TestingDatamodelRunner();
    }

    /**
     * Method to access the runner easily from tests.
     */
    protected DatamodelRunner runner() {
        if (runner == null) {
            runner = createDatamodelRunner();
        }
        return runner;
    }

    @BeforeSuite
    public void init() {
        System.setProperty("log4j.configuration", "tests.log4j.properties");
        try {
            workspaceDir = TestUtils.createTempDirectory("openrefine-test-workspace-dir");
            File jsonPath = new File(workspaceDir, "workspace.json");
            FileUtils.writeStringToFile(jsonPath, "{\"projectIDs\":[]\n" +
                    ",\"preferences\":{\"entries\":{\"scripting.starred-expressions\":" +
                    "{\"class\":\"org.openrefine.preference.TopList\",\"top\":2147483647," +
                    "\"list\":[]},\"scripting.expressions\":{\"class\":\"org.openrefine.preference.TopList\",\"top\":100,\"list\":[]}}}}",
                    "UTF-8"); // JSON is always UTF-8
            FileProjectManager.initialize(runner, workspaceDir);

        } catch (IOException e) {
            workspaceDir = null;
            e.printStackTrace();
        }

        // This just keeps track of any failed test, for cleanupWorkspace
        testFailed = false;
    }

    @BeforeMethod
    protected void initProjectManager() {
        ProjectManager.singleton = new ProjectManagerStub(runner());
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
    protected Project createProject(String[] columns, Serializable[][] rows) {
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
    protected Project createProject(String projectName, String[] columns, Serializable[][] rows) {
        GridState state = createGrid(columns, rows);
        return createProject(projectName, state);
    }

    protected Project createProject(String projectName, GridState grid) {
        ProjectMetadata meta = new ProjectMetadata();
        meta.setName(projectName);
        Project project = new Project(grid, new LazyChangeDataStore(), new LazyCachedGridStore());
        ProjectManager.singleton.registerProject(project, meta);
        return project;
    }

    protected GridState createGrid(String[] columns, Serializable[][] rows) {
        List<ColumnMetadata> columnMeta = new ArrayList<>(columns.length);
        for (String column : columns) {
            columnMeta.add(new ColumnMetadata(column));
        }
        ColumnModel model = new ColumnModel(columnMeta);
        Cell[][] cells = new Cell[rows.length][];
        for (int i = 0; i != rows.length; i++) {
            cells[i] = new Cell[columns.length];
            for (int j = 0; j != rows[i].length; j++) {
                if (rows[i][j] == null) {
                    cells[i][j] = null;
                } else if (rows[i][j] instanceof Cell) {
                    cells[i][j] = (Cell) rows[i][j];
                } else {
                    cells[i][j] = new Cell(rows[i][j], null);
                }
            }
        }

        return runner().create(model, toRows(cells), Collections.emptyMap());
    }

    @Deprecated
    protected Project createProject(String projectName, String[] columns, Serializable[] rows) {
        Serializable[][] cells = new Serializable[rows.length / columns.length][];
        for (int i = 0; i != rows.length; i++) {
            if (i % columns.length == 0) {
                cells[i / columns.length] = new Serializable[columns.length];
            }
            cells[i / columns.length][i % columns.length] = rows[i];
        }
        return createProject(projectName, columns, cells);
    }

    protected List<Row> toRows(Cell[][] cells) {
        List<Row> rows = new ArrayList<>(cells.length);
        for (int i = 0; i != cells.length; i++) {
            List<Cell> currentCells = new ArrayList<>(cells[i].length);
            for (int j = 0; j != cells[i].length; j++) {
                currentCells.add(cells[i][j]);
            }
            rows.add(new Row(currentCells));
        }
        return rows;
    }

    // We do not use the equals method of GridState here because GridState does not check for equality
    // with its grid contents (because this would require fetching all rows in memory)
    protected void assertGridEquals(GridState actual, GridState expected) {
        Assert.assertEquals(actual.getColumnModel(), expected.getColumnModel());
        Assert.assertEquals(actual.collectRows(), expected.collectRows());
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

        options.put("separator", sep);
        options.put("limit", limit);
        options.put("skipDataLines", skip);
        options.put("ignoreLines", ignoreLines);
        options.put("headerLines", headerLines);
        options.put("guessCellValueTypes", guessValueType);
        options.put("processQuotes", !ignoreQuotes);
        options.put("storeBlankCellsAsNulls", true);
    }

    /**
     * Cleans up the projects and jobs created with createCSVProject
     */
    @AfterMethod
    protected void cleanupProjectsAndJobs() {
        ProjectManager projectManager = ProjectManager.singleton;
        projectManager.dispose();
    }

    protected Recon testRecon(String name, String id, Recon.Judgment judgment) {
        List<ReconCandidate> candidates = Arrays.asList(
                new ReconCandidate(id, name + " 1", null, 98.0),
                new ReconCandidate(id + "2", name + " 2", null, 76.0));
        ReconCandidate match = Recon.Judgment.Matched.equals(judgment) ? candidates.get(0) : null;
        return new Recon(
                1234L,
                3478L,
                judgment,
                match,
                new Object[3],
                candidates,
                "http://my.service.com/api",
                "http://my.service.com/space",
                "http://my.service.com/schema",
                "batch",
                -1);
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
        Assert.assertNotNull(project.getHistory());
        Assert.assertNotNull(project.getHistory().getInitialGridState());
        ColumnModel model = project.getHistory().getInitialGridState().getColumnModel();
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getColumns().size(), numCols);
        Assert.assertEquals(project.getHistory().getInitialGridState().rowCount(), numRows);
    }

    /**
     * Parse and evaluate a GREL expression and compare the result to the expect value
     *
     * @param bindings
     * @param test
     * @throws ParsingException
     */
    protected void parseEval(Properties bindings, String[] test)
            throws ParsingException {
        Evaluable eval = MetaParser.parse("grel:" + test[0]);
        Object result = eval.evaluate(bindings);
        Assert.assertEquals(result.toString(), test[1], "Wrong result for expression: " + test[0]);
    }

    /**
     * Parse and evaluate a GREL expression and compare the result an expected type using instanceof
     *
     * @param bindings
     * @param test
     * @throws ParsingException
     */
    protected void parseEvalType(Properties bindings, String test, @SuppressWarnings("rawtypes") Class clazz)
            throws ParsingException {
        Evaluable eval = MetaParser.parse("grel:" + test);
        Object result = eval.evaluate(bindings);
        Assert.assertTrue(clazz.isInstance(result), "Wrong result type for expression: " + test);
    }

    @AfterMethod
    public void TearDown() throws Exception {
        bindings = null;
    }

}
