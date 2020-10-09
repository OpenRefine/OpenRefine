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
import java.util.Collections;
import java.util.Properties;

import org.openrefine.ProjectMetadata;
import org.openrefine.RefineTest;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class CsvExporterTests extends RefineTest {

    @BeforeTest
    public void initLogger() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    //dependencies
    StringWriter writer;
    Engine engine;
    GridState grid;
    ProjectMetadata projectMetadata;
    Properties options;

    //System Under Test
    CsvExporter SUT;
	

    @BeforeMethod
    public void SetUp(){
        SUT = new CsvExporter();
        projectMetadata = new ProjectMetadata();
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
    public void exportSimpleCsv(){
        CreateGrid(2, 2);

        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0,column1\n" +
                                               "row0cell0,row0cell1\n" +
                                               "row1cell0,row1cell1\n");

    }

    @Test
    public void exportSimpleCsvNoHeader(){
        CreateGrid(2, 2);
        when(options.getProperty("printColumnHeader")).thenReturn("false");
        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "row0cell0,row0cell1\n" +
                                               "row1cell0,row1cell1\n");

        verify(options,times(2)).getProperty("printColumnHeader");
    }
    
    @Test
    public void exportSimpleCsvCustomLineSeparator(){
        CreateGrid(2, 2);
        when(options.getProperty("options")).thenReturn("{\"lineSeparator\":\"X\"}");

        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0,column1X" +
                                               "row0cell0,row0cell1X" +
                                               "row1cell0,row1cell1X");

    }

    @Test
    public void exportCsvWithLineBreaks(){
        Project project = createProject("csv project",
        		new String[] {"column0","column1","column2"},
        		new Serializable[][] {
        	{"row0cell0","row0cell1","row0cell2"},
        	{"row1cell0","line\n\n\nbreak","row1cell2"},
        	{"row2cell0","row2cell1","row2cell2"}
        });
        
        GridState grid = project.getCurrentGridState();
        Engine engine = new Engine(grid, new EngineConfig(Collections.emptyList(), Engine.Mode.RowBased));
        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0,column1,column2\n" +
                                               "row0cell0,row0cell1,row0cell2\n" +
                                               "row1cell0,\"line\n\n\nbreak\",row1cell2\n" +
                                               "row2cell0,row2cell1,row2cell2\n");
    }

    @Test
    public void exportCsvWithComma(){
    	Project project = createProject("csv project",
        		new String[] {"column0","column1","column2"},
        		new Serializable[][] {
        	{"row0cell0","row0cell1","row0cell2"},
        	{"row1cell0","with, comma","row1cell2"},
        	{"row2cell0","row2cell1","row2cell2"}
        });
        
        GridState grid = project.getCurrentGridState();
        Engine engine = new Engine(grid, new EngineConfig(Collections.emptyList(), Engine.Mode.RowBased));
        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0,column1,column2\n" +
                                               "row0cell0,row0cell1,row0cell2\n" +
                                               "row1cell0,\"with, comma\",row1cell2\n" +
                                               "row2cell0,row2cell1,row2cell2\n");
    }

    @Test
    public void exportCsvWithQuote(){
    	Project project = createProject("csv project",
        		new String[] {"column0","column1","column2"},
        		new Serializable[][] {
        	{"row0cell0","row0cell1","row0cell2"},
        	{"row1cell0","line has \"quote\"","row1cell2"},
        	{"row2cell0","row2cell1","row2cell2"}
        });
        
        GridState grid = project.getCurrentGridState();
        Engine engine = new Engine(grid, new EngineConfig(Collections.emptyList(), Engine.Mode.RowBased));
        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0,column1,column2\n" +
                                               "row0cell0,row0cell1,row0cell2\n" +
                                               "row1cell0,\"line has \"\"quote\"\"\",row1cell2\n" +
                                               "row2cell0,row2cell1,row2cell2\n");
    }

    @Test
    public void exportCsvWithEmptyCells(){
    	Project project = createProject("csv project",
        		new String[] {"column0","column1","column2"},
        		new Serializable[][] {
        	{"row0cell0","row0cell1","row0cell2"},
        	{"row1cell0",null,"row1cell2"},
        	{null,"row2cell1","row2cell2"}
        });
        
        GridState grid = project.getCurrentGridState();
        Engine engine = new Engine(grid, new EngineConfig(Collections.emptyList(), Engine.Mode.RowBased));
        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0,column1,column2\n" +
                                               "row0cell0,row0cell1,row0cell2\n" +
                                               "row1cell0,,row1cell2\n" +
                                               ",row2cell1,row2cell2\n");
    }
    
    //helper methods

    protected void CreateGrid(int noOfRows, int noOfColumns){
    	String[] columns = new String[noOfColumns];
		for(int i = 0; i < noOfColumns; i++) {
			columns[i] = "column" + i;
		}
        
        Serializable[][] cells = new Serializable[noOfRows][];
        for(int i = 0; i < noOfRows; i++){
            cells[i] = new Serializable[noOfColumns];
            for(int j = 0; j < noOfColumns; j++){
                cells[i][j] = "row" + i + "cell" + j;
            }
        }
        
		grid = createGrid(columns, cells);
		engine = new Engine(grid, new EngineConfig(Collections.emptyList(), Engine.Mode.RowBased));
    }
}
