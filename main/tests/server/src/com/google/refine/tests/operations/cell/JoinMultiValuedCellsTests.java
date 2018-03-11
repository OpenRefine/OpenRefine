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
import com.google.refine.operations.cell.MultiValuedCellJoinOperation;
import com.google.refine.process.Process;
import com.google.refine.tests.RefineTest;


public class JoinMultiValuedCellsTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    /*
     * Test to demonstrate the intended behaviour of the function
     */

    @Test
    public void testJoinMultiValuedCells() throws Exception {
        Project project = createCSVProject(
                "Key,Value\n"
                + "Record_1,one\n"
                + ",two\n"
                + ",three\n"
                + ",four\n");

        AbstractOperation op = new MultiValuedCellJoinOperation(
            "Value",
            "Key",
            ",");
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();

        int keyCol = project.columnModel.getColumnByName("Key").getCellIndex();
        int valueCol = project.columnModel.getColumnByName("Value").getCellIndex();
        
        Assert.assertEquals(project.rows.get(0).getCellValue(keyCol), "Record_1");
        Assert.assertEquals(project.rows.get(0).getCellValue(valueCol), "one,two,three,four");
    }

    @Test
    public void testJoinMultiValuedCellsMultipleSpaces() throws Exception {
        Project project = createCSVProject(
            "Key,Value\n"
            + "Record_1,one\n"
            + ",two\n"
            + ",three\n"
            + ",four\n");


        AbstractOperation op = new MultiValuedCellJoinOperation(
            "Value",
            "Key",
            ",     ,");
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();

        int keyCol = project.columnModel.getColumnByName("Key").getCellIndex();
        int valueCol = project.columnModel.getColumnByName("Value").getCellIndex();
        
        Assert.assertEquals(project.rows.get(0).getCellValue(keyCol), "Record_1");
        Assert.assertEquals(project.rows.get(0).getCellValue(valueCol), "one,     ,two,     ,three,     ,four");
    }


}

