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

package org.openrefine.exporters;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Properties;

import org.openrefine.ProjectMetadata;
import org.openrefine.RefineTest;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.GridState;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class HtmlExporterTests extends RefineTest {

    private static final String TEST_PROJECT_NAME = "html table exporter test project";

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    //dependencies
    StringWriter writer;
    Engine engine;
    GridState grid;
    ProjectMetadata projectMetadata;
    Properties options;

    //System Under Test
    WriterExporter SUT;

    @BeforeMethod
    public void SetUp(){
        SUT = new HtmlTableExporter();
        projectMetadata = new ProjectMetadata();
        projectMetadata.setName(TEST_PROJECT_NAME);
        writer = new StringWriter();
        options = mock(Properties.class);
    }

    @AfterMethod
    public void TearDown(){
        SUT = null;
        writer = null;
        grid = null;
        engine = null;
        options = null;
    }

    @Test
    public void exportSimpleHtmlTable(){
    	grid = createGrid(new String[] {"column0", "column1"},
    			new Serializable[][] {
    		{"row0cell0", "row0cell1"},
    		{"row1cell0", "row1cell1"}
    	});
    	engine = new Engine(grid, EngineConfig.ALL_ROWS);

        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "<html>\n" +
                "<head>\n" + "<title>" + TEST_PROJECT_NAME + "</title>\n" + 
                "<meta charset=\"utf-8\" />\n" + 
                "</head>\n" +
                "<body>\n" +
                "<table>\n" +
                "<tr><th>column0</th><th>column1</th></tr>\n" +
                "<tr><td>row0cell0</td><td>row0cell1</td></tr>\n" +
                "<tr><td>row1cell0</td><td>row1cell1</td></tr>\n" +
                "</table>\n" +
                "</body>\n" +
        "</html>\n");
        
    }

    // TODO: This test fails because the HTML table exporter 
    // apparently doesn't honor the column header option.  Should it?
    @Test(enabled=false)
    public void exportSimpleHtmlTableNoHeader() {
    	grid = createGrid(new String[] {"column0", "column1"},
    			new Serializable[][] {
    		{"row0cell0", "row0cell1"},
    		{"row1cell0", "row1cell1"}
    	});
    	engine = new Engine(grid, EngineConfig.ALL_ROWS);
    	
        when(options.getProperty("printColumnHeader")).thenReturn("false");
        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "<html>\n" +
                "<head>\n" + "<title>" + TEST_PROJECT_NAME + "</title>\n" + 
                "<meta charset=\"utf-8\" />\n" + 
                "</head>\n" +
                "<body>\n" +
                "<table>\n" +
                "<tr><td>row0cell0</td><td>row0cell1</td></tr>\n" +
                "<tr><td>row1cell0</td><td>row1cell1</td></tr>\n" +
                "</table>\n" +
                "</body>\n" +
                "</html>\n");
        verify(options,times(2)).getProperty("printColumnHeader");
    }


    @Test
    public void exportHtmlTableWithEmptyCells() {
    	grid = createGrid(new String[] {"column0", "column1", "column2"},
    			new Serializable[][] {
    		{"row0cell0", "row0cell1", "row0cell2"},
    		{"row1cell0", null, "row1cell2"},
    		{null, "row2cell1", "row2cell2"}
    	});
    	engine = new Engine(grid, EngineConfig.ALL_ROWS);

        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "<html>\n" +
                "<head>\n" + "<title>" + TEST_PROJECT_NAME + "</title>\n" + 
                "<meta charset=\"utf-8\" />\n" +
                "</head>\n" +
                "<body>\n" +
                "<table>\n" +
                "<tr><th>column0</th><th>column1</th><th>column2</th></tr>\n" +
                "<tr><td>row0cell0</td><td>row0cell1</td><td>row0cell2</td></tr>\n" +
                "<tr><td>row1cell0</td><td></td><td>row1cell2</td></tr>\n" +
                "<tr><td></td><td>row2cell1</td><td>row2cell2</td></tr>\n" +
                "</table>\n" +
                "</body>\n" +
                "</html>\n");
        }
    
    @Test
    public void exportHtmlTableWithURLs() {
    	grid = createGrid(new String[] {"column0", "column1", "column2"},
    			new Serializable[][] {
    		{"row0cell0", "row0cell1", "row0cell2"},
    		{"row1cell0", "ftp://ftp.ripe.net/ripe/", "row1cell2"},
    		{"https://gnu.org/", "row2cell1", "row2cell2"}
    	});
    	engine = new Engine(grid, EngineConfig.ALL_ROWS);

        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "<html>\n" +
                "<head>\n" + "<title>" + TEST_PROJECT_NAME + "</title>\n" + 
                "<meta charset=\"utf-8\" />\n" +
                "</head>\n" +
                "<body>\n" +
                "<table>\n" +
                "<tr><th>column0</th><th>column1</th><th>column2</th></tr>\n" +
                "<tr><td>row0cell0</td><td>row0cell1</td><td>row0cell2</td></tr>\n" +
                "<tr><td>row1cell0</td><td><a href=\"ftp://ftp.ripe.net/ripe/\">ftp://ftp.ripe.net/ripe/</a></td><td>row1cell2</td></tr>\n" +
                "<tr><td><a href=\"https://gnu.org/\">https://gnu.org/</a></td><td>row2cell1</td><td>row2cell2</td></tr>\n" +
                "</table>\n" +
                "</body>\n" +
                "</html>\n");
    }
}
