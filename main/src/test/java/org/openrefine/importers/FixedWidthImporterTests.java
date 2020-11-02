/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.openrefine.importers;


import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.SparkDatamodelRunner;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class FixedWidthImporterTests extends ImporterTest {

    //constants
    String SAMPLE_ROW = "NDB_NoShrt_DescWater";

    //System Under Test
    FixedWidthImporter SUT = null;

    @Override
    @BeforeMethod
    public void setUp(){
        super.setUp();
        SUT = new FixedWidthImporter(runner());
    }

    @Override
    @AfterMethod
    public void tearDown(){
        SUT = null;
        super.tearDown();
    }
    
    //---------------------read tests------------------------
    @Test
    public void readFixedWidth() throws Exception {
    	File testFile = TestUtils.createTempFile("my_dataset.txt", "NDB_NoShrt_DescWater\nTooShort\n");
    	
        ArrayNode columnWidths = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(columnWidths, 6);
        JSONUtilities.append(columnWidths, 9);
        JSONUtilities.append(columnWidths, 5);
        whenGetArrayOption("columnWidths", options, columnWidths);

        ArrayNode columnNames = ParsingUtilities.mapper.createArrayNode();
        columnNames.add("Col 1");
        columnNames.add("Col 2"); 
        columnNames.add("Col 3");
        whenGetArrayOption("columnNames", options, columnNames);

        whenGetIntegerOption("ignoreLines", options, 0);
        whenGetIntegerOption("headerLines", options, 0);
        whenGetIntegerOption("skipDataLines", options, 0);
        whenGetIntegerOption("limit", options, -1);
        whenGetBooleanOption("storeBlankCellsAsNulls",options,true);
        
        GridState result = parseOneFile(SUT, testFile.getAbsolutePath());
        
        List<IndexedRow> rows = result.collectRows();
        Assert.assertEquals(rows.size(), 2);
        Assert.assertEquals(rows.get(0).getRow().cells.size(), 3);
        Assert.assertEquals((String)rows.get(0).getRow().getCellValue(0), "NDB_No");
        Assert.assertEquals((String)rows.get(0).getRow().getCellValue(1), "Shrt_Desc");
        Assert.assertEquals((String)rows.get(0).getRow().getCellValue(2), "Water");
        Assert.assertEquals(rows.get(0).getRow().cells.size(), 3);
        Assert.assertEquals((String)rows.get(1).getRow().getCellValue(0), "TooSho");
        Assert.assertEquals((String)rows.get(1).getRow().getCellValue(1), "rt");
        Assert.assertNull(rows.get(1).getRow().getCellValue(2));
        
        Assert.assertEquals(result.getColumnModel().getColumnNames(), Arrays.asList("Col 1", "Col 2", "Col 3"));
    }
    
    @Test
    public void readNoColumnNames() throws Exception {
    	File testFile = TestUtils.createTempFile("my_dataset.txt", "NDB_NoShrt_DescWater\nTooShort\n");
    	
        ArrayNode columnWidths = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(columnWidths, 6);
        JSONUtilities.append(columnWidths, 9);
        JSONUtilities.append(columnWidths, 5);
        whenGetArrayOption("columnWidths", options, columnWidths);

        whenGetIntegerOption("ignoreLines", options, 0);
        whenGetIntegerOption("headerLines", options, 0);
        whenGetIntegerOption("skipDataLines", options, 0);
        whenGetIntegerOption("limit", options, -1);
        whenGetBooleanOption("storeBlankCellsAsNulls",options,true);
        
        GridState result = parseOneFile(SUT, testFile.getAbsolutePath());
        
        List<IndexedRow> rows = result.collectRows();
        Assert.assertEquals(rows.size(), 2);
        Assert.assertEquals(rows.get(0).getRow().cells.size(), 3);
        Assert.assertEquals((String)rows.get(0).getRow().getCellValue(0), "NDB_No");
        Assert.assertEquals((String)rows.get(0).getRow().getCellValue(1), "Shrt_Desc");
        Assert.assertEquals((String)rows.get(0).getRow().getCellValue(2), "Water");
        Assert.assertEquals(rows.get(0).getRow().cells.size(), 3);
        Assert.assertEquals((String)rows.get(1).getRow().getCellValue(0), "TooSho");
        Assert.assertEquals((String)rows.get(1).getRow().getCellValue(1), "rt");
        Assert.assertNull(rows.get(1).getRow().getCellValue(2));
        
        Assert.assertEquals(result.getColumnModel().getColumnNames(), Arrays.asList("Column 1", "Column 2", "Column 3"));
    }
    
    @Test
    public void readColumnHeader() throws Exception {
    	File testFile = TestUtils.createTempFile("my_dataset.txt", "NDB_NoShrt_DescWater\n012345green....00342\n");
    	
        ArrayNode columnWidths = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(columnWidths, 6);
        JSONUtilities.append(columnWidths, 9);
        JSONUtilities.append(columnWidths, 5);
        whenGetArrayOption("columnWidths", options, columnWidths);

        whenGetIntegerOption("ignoreLines", options, 0);
        whenGetIntegerOption("headerLines", options, 1);
        whenGetIntegerOption("skipDataLines", options, 0);
        whenGetIntegerOption("limit", options, -1);
        whenGetBooleanOption("storeBlankCellsAsNulls",options,true);
        
        GridState result = parseOneFile(SUT, testFile.getAbsolutePath());
        
        List<IndexedRow> rows = result.collectRows();
        Assert.assertEquals(rows.size(), 1);
        Assert.assertEquals(rows.get(0).getRow().cells.size(), 3);
        Assert.assertEquals((String)rows.get(0).getRow().getCellValue(0), "012345");
        Assert.assertEquals((String)rows.get(0).getRow().getCellValue(1), "green....");
        Assert.assertEquals((String)rows.get(0).getRow().getCellValue(2), "00342");
        
        Assert.assertEquals(result.getColumnModel().getColumnNames(), Arrays.asList("NDB_No", "Shrt_Desc", "Water"));
    }
}
