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

package com.google.refine.operations.column;

import java.util.Arrays;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.process.Process;
import com.google.refine.util.TestUtils;

public class ColumnReorderOperationTests extends RefineTest {

    Project project;

    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation(getCoreModule(), "column-reorder", ColumnReorderOperation.class);
    }

    @BeforeMethod
    public void createProject() {
        project = createCSVProject(
                "a,b,c\n" +
                        "1|2,d,e\n" +
                        "3,f,g\n");
    }

    @Test
    public void serializeColumnReorderOperation() {
        AbstractOperation op = new ColumnReorderOperation(Arrays.asList("b", "c", "a"));
        TestUtils.isSerializedTo(op, "{\"op\":\"core/column-reorder\","
                + "\"description\":\"Reorder columns\","
                + "\"columnNames\":[\"b\",\"c\",\"a\"]}");
    }

    @Test
    public void testEraseCellsOnRemovedColumns() throws Exception {

        int aCol = project.columnModel.getColumnByName("a").getCellIndex();
        int bCol = project.columnModel.getColumnByName("b").getCellIndex();
        int cCol = project.columnModel.getColumnByName("c").getCellIndex();

        Assert.assertEquals(project.rows.get(0).getCellValue(aCol), "1|2");
        Assert.assertEquals(project.rows.get(0).getCellValue(bCol), "d");
        Assert.assertEquals(project.rows.get(0).getCellValue(cCol), "e");
        Assert.assertEquals(project.rows.get(1).getCellValue(aCol), "3");
        Assert.assertEquals(project.rows.get(1).getCellValue(bCol), "f");
        Assert.assertEquals(project.rows.get(1).getCellValue(cCol), "g");

        AbstractOperation op = new ColumnReorderOperation(Arrays.asList("a"));
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();

        Assert.assertEquals(project.rows.get(0).getCellValue(aCol), "1|2");
        Assert.assertEquals(project.rows.get(0).getCellValue(bCol), null);
        Assert.assertEquals(project.rows.get(0).getCellValue(cCol), null);
        Assert.assertEquals(project.rows.get(1).getCellValue(aCol), "3");
        Assert.assertEquals(project.rows.get(1).getCellValue(bCol), null);
        Assert.assertEquals(project.rows.get(1).getCellValue(cCol), null);

    }
}
