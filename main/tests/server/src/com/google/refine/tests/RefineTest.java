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

package com.google.refine.tests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import com.google.refine.ProjectManager;
import com.google.refine.RefineServlet;
import com.google.refine.importers.SeparatorBasedImporter;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.io.FileProjectManager;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.medadata.ProjectMetadata;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.JSONUtilities;

/**
 * A base class containing various utilities to help testing Refine.
 */
public class RefineTest {

    protected Logger logger;
    
    boolean testFailed;
    protected File workspaceDir;
    protected RefineServlet servlet;
    private List<Project> projects = new ArrayList<Project>();
    private List<ImportingJob> importingJobs = new ArrayList<ImportingJob>();

    @BeforeSuite
    public void init() {
        System.setProperty("log4j.configuration", "tests.log4j.properties");
        try {
            workspaceDir = TestUtils.createTempDirectory("openrefine-test-workspace-dir");
            File jsonPath = new File(workspaceDir, "workspace.json");
            FileUtils.writeStringToFile(jsonPath, "{\"projectIDs\":[]\n" + 
                    ",\"preferences\":{\"entries\":{\"scripting.starred-expressions\":" +
                    "{\"class\":\"com.google.refine.preference.TopList\",\"top\":2147483647," +
                    "\"list\":[]},\"scripting.expressions\":{\"class\":\"com.google.refine.preference.TopList\",\"top\":100,\"list\":[]}}}}");
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
        servlet = new RefineServletStub();
        ProjectManager.singleton = new ProjectManagerStub();
        ImportingManager.initialize(servlet);
    }
    
    protected Project createProjectWithColumns(String projectName, String... columnNames) throws IOException, ModelException {
        Project project = new Project();
        ProjectMetadata pm = new ProjectMetadata();
        pm.setName(projectName);
        ProjectManager.singleton.registerProject(project, pm);

        if (columnNames != null) {
            for(String columnName : columnNames) {
                int index = project.columnModel.allocateNewCellIndex();
                Column column = new Column(index,columnName);
                project.columnModel.addColumn(index, column, true);
            }
        }
        return project;
    }
    
    /**
     * Helper to create a project from a CSV encoded as a file. Not much
     * control is given on the import options, because this method is intended
     * to be a quick way to create a project for a test. For more control over
     * the import, just call the importer directly.
     * 
     * @param input
     *          contents of the CSV file to create the project from
     * @return
     */
    protected Project createCSVProject(String input) {
        return createCSVProject("test project", input);
    }
    
    /**
     * Helper to create a project from a CSV encoded as a file. Not much
     * control is given on the import options, because this method is intended
     * to be a quick way to create a project for a test. For more control over
     * the import, just call the importer directly.
     * 
     * The projects created via this method and their importing jobs will be disposed of
     * at the end of each test.
     * 
     * @param projectName
     *       the name of the project to create
     * @param input
     *       the content of the file, encoded as a CSV (with "," as a separator)
     * @return
     */
    protected Project createCSVProject(String projectName, String input) {

        
        Project project = new Project();
        
        ProjectMetadata metadata = new ProjectMetadata();
        metadata.setName(projectName);
        
        JSONObject options = mock(JSONObject.class);
        prepareImportOptions(options, ",", -1, 0, 0, 1, false, false);
        
        ImportingJob job = ImportingManager.createJob();
        
        SeparatorBasedImporter importer = new SeparatorBasedImporter();
        
        List<Exception> exceptions = new ArrayList<Exception>();
        importer.parseOneFile(project, metadata, job, "filesource", new StringReader(input), -1, options, exceptions);
        project.update();
        ProjectManager.singleton.registerProject(project, metadata);
        
        projects.add(project);
        importingJobs.add(job);
        return project;
    }
    
    /**
     * Initializes the importing options for the CSV importer.
     * @param options
     * @param sep
     * @param limit
     * @param skip
     * @param ignoreLines
     * @param headerLines
     * @param guessValueType
     * @param ignoreQuotes
     */
    private void prepareImportOptions(JSONObject options,
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
        for(ImportingJob job : importingJobs) {
            ImportingManager.disposeJob(job.id);
        }
        for(Project project: projects) {
            ProjectManager.singleton.deleteProject(project.id);
        }
        servlet = null;
    }
            
    /**
     * Check that a project was created with the appropriate number of columns and rows.
     * 
     * @param project project to check
     * @param numCols expected column count
     * @param numRows expected row count
     */
    public static void assertProjectCreated(Project project, int numCols, int numRows) {
        Assert.assertNotNull(project);
        Assert.assertNotNull(project.columnModel);
        Assert.assertNotNull(project.columnModel.columns);
        Assert.assertEquals(project.columnModel.columns.size(), numCols);
        Assert.assertNotNull(project.rows);
        Assert.assertEquals(project.rows.size(), numRows);
    }

    /**
     * Check that a project was created with the appropriate number of columns, rows, and records.
     * 
     * @param project project to check
     * @param numCols expected column count
     * @param numRows expected row count
     * @param numRows expected record count
     */
    public static void assertProjectCreated(Project project, int numCols, int numRows, int numRecords) {
        assertProjectCreated(project,numCols,numRows);
        Assert.assertNotNull(project.recordModel);
        Assert.assertEquals(project.recordModel.getRecordCount(),numRecords);
    }

    public void log(Project project) {
        // some quick and dirty debugging
        StringBuilder sb = new StringBuilder();
        for(Column c : project.columnModel.columns){
            sb.append(c.getName());
            sb.append("; ");
        }
        logger.info(sb.toString());
        for(Row r : project.rows){
            sb = new StringBuilder();
            for(int i = 0; i < r.cells.size(); i++){
                Cell c = r.getCell(i);
                if(c != null){
                   sb.append(c.value);
                   sb.append("; ");
                }else{
                    sb.append("null; ");
                }
            }
            logger.info(sb.toString());
        }
    }
    
    //----helpers----
    
    static public void whenGetBooleanOption(String name, JSONObject options, Boolean def){
        when(options.has(name)).thenReturn(true);
        when(JSONUtilities.getBoolean(options, name, def)).thenReturn(def);
    }
    
    static public void whenGetIntegerOption(String name, JSONObject options, int def){
        when(options.has(name)).thenReturn(true);
        when(JSONUtilities.getInt(options, name, def)).thenReturn(def);
    }
    
    static public void whenGetStringOption(String name, JSONObject options, String def){
        when(options.has(name)).thenReturn(true);
        when(JSONUtilities.getString(options, name, def)).thenReturn(def);
    }
    
    static public void whenGetObjectOption(String name, JSONObject options, JSONObject def){
        when(options.has(name)).thenReturn(true);
        when(JSONUtilities.getObject(options, name)).thenReturn(def);
    }
    
    static public void whenGetArrayOption(String name, JSONObject options, JSONArray def){
        when(options.has(name)).thenReturn(true);
        when(JSONUtilities.getArray(options, name)).thenReturn(def);
    }
    
    static public void verifyGetOption(String name, JSONObject options){
        verify(options, times(1)).has(name);
        try {
            verify(options, times(1)).get(name);
        } catch (JSONException e) {
            Assert.fail("JSONException",e);
        }
    }
    
    // Works for both int, String, and JSON arrays
    static public void verifyGetArrayOption(String name, JSONObject options){
        verify(options, times(1)).has(name);
        try {
            verify(options, times(1)).getJSONArray(name);
        } catch (JSONException e) {
            Assert.fail("JSONException",e);
        }
    }
}
