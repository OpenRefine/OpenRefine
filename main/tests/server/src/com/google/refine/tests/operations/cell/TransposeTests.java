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

package com.google.refine.tests.operations.cell;

import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.model.medadata.ProjectMetadata;
import com.google.refine.operations.cell.KeyValueColumnizeOperation;
import com.google.refine.process.Process;
import com.google.refine.tests.RefineTest;

public class TransposeTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Test in the case where an ID is available in the first column.
     * @throws Exception
     */
    @Test
    public void testKeyValueColumnizeWithID() throws Exception {
        Project project = createCSVProject(
                "ID,Cat,Val\n"
                + "1,a,1\n"
                + "1,b,3\n"
                + "2,b,4\n"
                + "2,c,5\n"
                + "3,a,2\n"
                + "3,b,5\n"
                + "3,d,3\n");

        AbstractOperation op = new KeyValueColumnizeOperation(
                "Cat", "Val", null);

        Process process = op.createProcess(project, new Properties());
        
        process.performImmediate();
            
        // Expected output from the GUI. 
        // ID,a,b,c,d
        // 1,1,3,,
        // 2,,4,5,
        // 3,2,5,,3
        Assert.assertEquals(project.columnModel.columns.size(), 5);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "ID");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "a");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "b");
        Assert.assertEquals(project.columnModel.columns.get(3).getName(), "c");
        Assert.assertEquals(project.columnModel.columns.get(4).getName(), "d");
        Assert.assertEquals(project.rows.size(), 3);
        
        // The actual row data structure has to leave the columns model untouched for redo/undo purpose.
        // So we have 2 empty columns(column 1,2) on the row level.
        // 1,1,3,,
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "1");
        Assert.assertEquals(project.rows.get(0).cells.get(3).value, "1");
        Assert.assertEquals(project.rows.get(0).cells.get(4).value, "3");
        
        // 2,,4,5,
        Assert.assertEquals(project.rows.get(1).cells.get(0).value, "2");
        Assert.assertEquals(project.rows.get(1).cells.get(4).value, "4");
        Assert.assertEquals(project.rows.get(1).cells.get(5).value, "5");
        
        // 3,2,5,,3
        Assert.assertEquals(project.rows.get(2).cells.get(0).value, "3");
        Assert.assertEquals(project.rows.get(2).cells.get(3).value, "2");
        Assert.assertEquals(project.rows.get(2).cells.get(4).value, "5");
        Assert.assertEquals(project.rows.get(2).cells.get(6).value, "3");
    }
    
    /**
     * Test to demonstrate the intended behaviour of the function when no id is available, for issue #1214
     * https://github.com/OpenRefine/OpenRefine/issues/1214
     */
    @Test
    public void testKeyValueColumnizeWithoutID() throws Exception {
        Project project = createCSVProject(
                "Key,Value\n"
                + "merchant,Katie\n"
                + "fruit,apple\n"
                + "price,1.2\n"
                + "fruit,pear\n"
                + "price,1.5\n"
                + "merchant,John\n"
                + "fruit,banana\n"
                + "price,3.1\n");

        AbstractOperation op = new KeyValueColumnizeOperation(
                "Key",
                "Value",
                null);
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();

        int merchantCol = project.columnModel.getColumnByName("merchant").getCellIndex();
        int fruitCol = project.columnModel.getColumnByName("fruit").getCellIndex();
        int priceCol = project.columnModel.getColumnByName("price").getCellIndex();
        
        Assert.assertEquals(project.rows.get(0).getCellValue(merchantCol), "Katie");
        Assert.assertEquals(project.rows.get(1).getCellValue(merchantCol), null);
        Assert.assertEquals(project.rows.get(2).getCellValue(merchantCol), "John");
        Assert.assertEquals(project.rows.get(0).getCellValue(fruitCol), "apple");
        Assert.assertEquals(project.rows.get(1).getCellValue(fruitCol), "pear");
        Assert.assertEquals(project.rows.get(2).getCellValue(fruitCol), "banana");
        Assert.assertEquals(project.rows.get(0).getCellValue(priceCol), "1.2");
        Assert.assertEquals(project.rows.get(1).getCellValue(priceCol), "1.5");
        Assert.assertEquals(project.rows.get(2).getCellValue(priceCol), "3.1");
    }
}
