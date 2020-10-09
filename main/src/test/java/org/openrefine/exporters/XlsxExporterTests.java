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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Properties;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

public class XlsxExporterTests extends RefineTest {

    private static final String TEST_PROJECT_NAME = "xlsx exporter test project";
    
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    //dependencies
    ByteArrayOutputStream stream;
    ProjectMetadata projectMetadata;
    GridState grid;
    Engine engine;
    Properties options;

    //System Under Test
    StreamExporter SUT;

    @BeforeMethod
    public void SetUp(){
        SUT = new XlsExporter(true);
        stream = new ByteArrayOutputStream();
        projectMetadata = new ProjectMetadata();
        projectMetadata.setName(TEST_PROJECT_NAME);
        options = mock(Properties.class);
    }

    @AfterMethod
    public void TearDown(){
        SUT = null;
        stream = null;
        grid = null;
        engine = null;
        options = null;
    }

    @Test
    public void exportSimpleXlsx() {
    	grid = createGrid(new String[] {"column0", "column1"},
    			new Serializable[][] {
    		{"row0cell0", "row0cell1"},
    		{"row1cell0", "row1cell1"}
    	});
    	engine = new Engine(grid, EngineConfig.ALL_ROWS);

        try {
            SUT.export(grid, projectMetadata, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }

        ByteArrayInputStream inStream = new ByteArrayInputStream(stream.toByteArray());
        try {
            XSSFWorkbook wb = new XSSFWorkbook(inStream);
            XSSFSheet ws = wb.getSheetAt(0);
            XSSFRow row1 = ws.getRow(1);
            XSSFCell cell0 = row1.getCell(0);
            Assert.assertEquals(cell0.toString(),"row0cell0");
            wb.close();
        } catch (IOException e) {
            Assert.fail();
        }
    }
    
    @Test
    public void exportXlsxDateType() throws IOException{
        OffsetDateTime odt =  OffsetDateTime.parse("2019-04-09T12:00+00:00");
        grid = createGrid(new String[] {"column0", "column1"},
    			new Serializable[][] {
    		{odt, odt},
    		{odt, odt}
    	});
    	engine = new Engine(grid, EngineConfig.ALL_ROWS);

        try {
            SUT.export(grid, projectMetadata, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }
        
        ByteArrayInputStream inStream = new ByteArrayInputStream(stream.toByteArray());
        try {
            XSSFWorkbook wb = new XSSFWorkbook(inStream);
            XSSFSheet ws = wb.getSheetAt(0);
            XSSFRow row1 = ws.getRow(1);
            XSSFCell cell0 = row1.getCell(0);
            Assert.assertTrue(cell0.toString().contains("2019"));
            wb.close();
        } catch (IOException e) {
            Assert.fail();
        }
    }
    
    @Test
    public void exportXlsxStringWithURLs() throws IOException{
        String url = "GET /primo-library/,http:%2F%2Fcatalogue.unice.fr HTTP/1.1";
        grid = createGrid(new String[] {"column0", "column1"},
    			new Serializable[][] {
    		{url, url},
    		{url, url}
    	});
        engine = new Engine(grid, EngineConfig.ALL_ROWS);

        try {
            SUT.export(grid, projectMetadata, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }
        
        ByteArrayInputStream inStream = new ByteArrayInputStream(stream.toByteArray());
        try {
            XSSFWorkbook wb = new XSSFWorkbook(inStream);
            XSSFSheet ws = wb.getSheetAt(0);
            XSSFRow row1 = ws.getRow(1);
            XSSFCell cell0 = row1.getCell(0);
            Assert.assertTrue(cell0.toString().contains("primo-library"));
            wb.close();
        } catch (IOException e) {
            Assert.fail();
        }
    }

}
