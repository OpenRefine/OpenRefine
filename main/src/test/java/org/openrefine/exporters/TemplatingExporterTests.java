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

public class TemplatingExporterTests extends RefineTest {

    private static final String TEST_PROJECT_NAME = "templating exporter test project";

    String rowPrefix = "boilerplate";
    String cellSeparator = "spacer";
    String prefix = "test prefix>";
    String suffix = "<test suffix";
    String rowSeparator = "\n";

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    //dependencies
    StringWriter writer;
    GridState grid;
    ProjectMetadata projectMetadata;
    Engine engine;
    Properties options;

    //System Under Test
    WriterExporter SUT;

    @BeforeMethod
    public void SetUp(){
        SUT = new TemplatingExporter();
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
    public void exportEmptyTemplate(){
    	grid = createGrid(new String[] {"foo"}, new Serializable[][] {});
    	engine = new Engine(grid, EngineConfig.ALL_ROWS);

        when(options.getProperty("template")).thenReturn("a template that should never get used");
        when(options.getProperty("prefix")).thenReturn(prefix);
        when(options.getProperty("suffix")).thenReturn(suffix);
        when(options.getProperty("separator")).thenReturn(rowSeparator);

        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), prefix + suffix);
    }
    
    @Test
    public void exportSimpleTemplate() {
    	grid = createGrid(new String[] {"column0", "column1"},
    			new Serializable[][] {
    		{"row0cell0", "row0cell1"},
    		{"row1cell0", "row1cell1"}
    	});
    	engine = new Engine(grid, EngineConfig.ALL_ROWS);

        String template = rowPrefix + "${column0}" + cellSeparator + "${column1}";

        when(options.getProperty("template")).thenReturn(template);
        when(options.getProperty("prefix")).thenReturn(prefix);
        when(options.getProperty("suffix")).thenReturn(suffix);
        when(options.getProperty("separator")).thenReturn(rowSeparator);

        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), 
                prefix 
                + rowPrefix + "row0cell0" + cellSeparator + "row0cell1" + rowSeparator
                + rowPrefix + "row1cell0" + cellSeparator + "row1cell1" 
                + suffix);
    }


    @Test()
    public void exportTemplateWithEmptyCells(){
    	grid = createGrid(new String[] {"column0", "column1", "column2"},
    			new Serializable[][] {
    		{"row0cell0", "row0cell1", "row0cell2"},
    		{"row1cell0", null, "row1cell2"},
    		{null, "row2cell1", "row2cell2"}
    	});
    	engine = new Engine(grid, EngineConfig.ALL_ROWS);
    	
        when(options.getProperty("template")).thenReturn(rowPrefix + "${column0}" + cellSeparator + "${column1}" + cellSeparator + "${column2}");
        when(options.getProperty("prefix")).thenReturn(prefix);
        when(options.getProperty("suffix")).thenReturn(suffix);
        when(options.getProperty("separator")).thenReturn(rowSeparator);

        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        // Template exporter returns null for empty cells
        Assert.assertEquals(writer.toString(), 
                prefix 
                + rowPrefix + "row0cell0" + cellSeparator + "row0cell1" + cellSeparator + "row0cell2" + rowSeparator
                + rowPrefix + "row1cell0" + cellSeparator + "null"    + cellSeparator + "row1cell2" + rowSeparator 
                + rowPrefix + "null"      + cellSeparator + "row2cell1" + cellSeparator + "row2cell2" 
                + suffix);

    }

    @Test()
    public void exportTemplateWithLimit() {
    	grid = createGrid(new String[] {"column0", "column1", "column2"},
    			new Serializable[][] {
    		{"row0cell0", "row0cell1", "row0cell2"},
    		{"row1cell0", "row1cell1", "row1cell2"},
    		{"row2cell0", "row2cell1", "row2cell2"}
    	});
    	engine = new Engine(grid, EngineConfig.ALL_ROWS);
        
        when(options.getProperty("limit")).thenReturn("2"); // optional integer
        when(options.getProperty("template")).thenReturn(rowPrefix + "${column0}" + cellSeparator + "${column1}" + cellSeparator + "${column2}");
        when(options.getProperty("prefix")).thenReturn(prefix);
        when(options.getProperty("suffix")).thenReturn(suffix);
        when(options.getProperty("separator")).thenReturn(rowSeparator);

        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), 
                prefix 
                + rowPrefix + "row0cell0" + cellSeparator + "row0cell1" + cellSeparator + "row0cell2" + rowSeparator
                + rowPrefix + "row1cell0" + cellSeparator + "row1cell1" + cellSeparator + "row1cell2"  
                // third row should be skipped because of limit
                + suffix);

    }
}
