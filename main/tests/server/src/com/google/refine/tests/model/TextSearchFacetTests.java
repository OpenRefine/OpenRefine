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

package com.google.refine.tests.model;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.RefineServlet;
import com.google.refine.browsing.RowFilter;
import com.google.refine.browsing.facets.TextSearchFacet;
import com.google.refine.importers.SeparatorBasedImporter;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.io.FileProjectManager;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.metadata.ProjectMetadata;
import com.google.refine.tests.RefineServletStub;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;


public class TextSearchFacetTests extends RefineTest {
    // dependencies
    private RefineServlet servlet;
    private Project project;
    private ProjectMetadata pm;
    private JSONObject options;
    private ImportingJob job;
    private SeparatorBasedImporter importer;
    private TextSearchFacet textfilter;
    private JSONObject textsearchfacet;
    private RowFilter rowfilter;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void setUp() throws JSONException, IOException, ModelException {
        servlet = new RefineServletStub();
        File dir = TestUtils.createTempDirectory("openrefine-test-workspace-dir");
        FileProjectManager.initialize(dir);
        project = new Project();
        pm = new ProjectMetadata();
        pm.setName("TextSearchFacet test");
        ProjectManager.singleton.registerProject(project, pm);
        options = mock(JSONObject.class);

        ImportingManager.initialize(servlet);
        job = ImportingManager.createJob();
        importer = new SeparatorBasedImporter();

        String csv = "Value\n"
            + "a\n"
            + "b\n"
            + "ab\n"
            + "Abc\n";
        prepareOptions(",", 10, 0, 0, 1, false, false);
        List<Exception> exceptions = new ArrayList<Exception>();
        importer.parseOneFile(project, pm, job, "filesource", new StringReader(csv), -1, options, exceptions);
        project.update();
        ProjectManager.singleton.registerProject(project, pm);
    }

    @AfterMethod
    public void tearDown() {
        ImportingManager.disposeJob(job.id);
        ProjectManager.singleton.deleteProject(project.id);
        job = null;
        project = null;
        pm = null;
        options = null;
    }

    /**
     * Test to demonstrate the intended behaviour of the function
     */

    @Test
    public void testTextFilter() throws Exception {
        //Apply text filter "a"

        //Column: "Value"
        //Filter Query: "a"
        //Mode: "text"
        //Case sensitive: False
        //Invert: False
        String filter =     "{\"type\":\"text\","
                            + "\"name\":\"Value\","
                            + "\"columnName\":\"Value\","
                            + "\"mode\":\"text\","
                            + "\"caseSensitive\":false,"
                            + "\"invert\":false,"
                            + "\"query\":\"a\"}";
        
        //Add the facet to the project and create a row filter
        textfilter = new TextSearchFacet();
        textsearchfacet = new JSONObject(filter);
        textfilter.initializeFromJSON(project,textsearchfacet);
        rowfilter = textfilter.getRowFilter(project);

        //Check each row in the project against the filter
        Assert.assertEquals(rowfilter.filterRow(project, 0, project.rows.get(0)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 1, project.rows.get(1)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 2, project.rows.get(2)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 3, project.rows.get(3)),true);
    }

    @Test
    public void testInvertedTextFilter() throws Exception {
        //Apply inverted text filter "a"

        //Column: "Value"
        //Filter Query: "a"
        //Mode: "text"
        //Case sensitive: False
        //Invert: True
        String filter =     "{\"type\":\"text\","
                            + "\"name\":\"Value\","
                            + "\"columnName\":\"Value\","
                            + "\"mode\":\"text\","
                            + "\"caseSensitive\":false,"
                            + "\"invert\":true,"
                            + "\"query\":\"a\"}";
        
        //Add the facet to the project and create a row filter
        textfilter = new TextSearchFacet();
        textsearchfacet = new JSONObject(filter);
        textfilter.initializeFromJSON(project,textsearchfacet);
        rowfilter = textfilter.getRowFilter(project);

        //Check each row in the project against the filter
        Assert.assertEquals(rowfilter.filterRow(project, 0, project.rows.get(0)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 1, project.rows.get(1)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 2, project.rows.get(2)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 3, project.rows.get(3)),false);
    }

    @Test
    public void testRegExFilter() throws Exception {
        //Apply regular expression filter "[bc]"

        //Column: "Value"
        //Filter Query: "[bc]"
        //Mode: "regex"
        //Case sensitive: False
        //Invert: False
        String filter =     "{\"type\":\"text\","
                            + "\"name\":\"Value\","
                            + "\"columnName\":\"Value\","
                            + "\"mode\":\"regex\","
                            + "\"caseSensitive\":false,"
                            + "\"invert\":false,"
                            + "\"query\":\"[bc]\"}";
        
        //Add the facet to the project and create a row filter
        textfilter = new TextSearchFacet();
        textsearchfacet = new JSONObject(filter);
        textfilter.initializeFromJSON(project,textsearchfacet);
        rowfilter = textfilter.getRowFilter(project);

        //Check each row in the project against the filter
        Assert.assertEquals(rowfilter.filterRow(project, 0, project.rows.get(0)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 1, project.rows.get(1)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 2, project.rows.get(2)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 3, project.rows.get(3)),true);
    }

    @Test
    public void testCaseSensitiveFilter() throws Exception {
        //Apply case-sensitive filter "A"

        //Column: "Value"
        //Filter Query: "A"
        //Mode: "text"
        //Case sensitive: True
        //Invert: False
        String filter =     "{\"type\":\"text\","
                            + "\"name\":\"Value\","
                            + "\"columnName\":\"Value\","
                            + "\"mode\":\"text\","
                            + "\"caseSensitive\":true,"
                            + "\"invert\":false,"
                            + "\"query\":\"A\"}";

        //Add the facet to the project and create a row filter
        textfilter = new TextSearchFacet();
        textsearchfacet = new JSONObject(filter);
        textfilter.initializeFromJSON(project,textsearchfacet);
        rowfilter = textfilter.getRowFilter(project);

        //Check each row in the project against the filter
        //Expect to retrieve one row containing "Abc"
        Assert.assertEquals(rowfilter.filterRow(project, 0, project.rows.get(0)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 1, project.rows.get(1)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 2, project.rows.get(2)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 3, project.rows.get(3)),true);
    }

    private void prepareOptions(
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


}

