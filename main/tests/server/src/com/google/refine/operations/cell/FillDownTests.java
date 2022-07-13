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

package com.google.refine.operations.cell;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.RefineTest;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class FillDownTests extends RefineTest {

    Project project = null;

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "fill-down", FillDownOperation.class);
    }

    @BeforeMethod
    public void setUp() {
        project = createCSVProject(
                "key,first,second\n" +
                        "a,b,c\n" +
                        ",d,\n" +
                        "e,f,\n" +
                        ",,h\n");
    }

    @AfterMethod
    public void tearDown() {
        ProjectManager.singleton.deleteProject(project.id);
    }

    @Test
    public void serializeFillDownOperation() throws Exception {
        String json = "{\"op\":\"core/fill-down\","
                + "\"description\":\"Fill down cells in column my key\","
                + "\"engineConfig\":{\"mode\":\"record-based\",\"facets\":[]},"
                + "\"columnName\":\"my key\"}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, FillDownOperation.class), json);
    }

    @Test
    public void testFillDownRecordKey() throws Exception {
        AbstractOperation op = new FillDownOperation(
                EngineConfig.reconstruct("{\"mode\":\"record-based\",\"facets\":[]}"),
                "key");
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();

        Assert.assertEquals("a", project.rows.get(0).cells.get(0).value);
        Assert.assertEquals("a", project.rows.get(1).cells.get(0).value);
        Assert.assertEquals("e", project.rows.get(2).cells.get(0).value);
        Assert.assertEquals("e", project.rows.get(3).cells.get(0).value);
    }

    // For issue #742
    // https://github.com/OpenRefine/OpenRefine/issues/742
    @Test
    public void testFillDownRecords() throws Exception {
        AbstractOperation op = new FillDownOperation(
                EngineConfig.reconstruct("{\"mode\":\"record-based\",\"facets\":[]}"),
                "second");
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();

        Assert.assertEquals("c", project.rows.get(0).cells.get(2).value);
        Assert.assertEquals("c", project.rows.get(1).cells.get(2).value);
        Assert.assertNull(project.rows.get(2).cells.get(2));
        Assert.assertEquals("h", project.rows.get(3).cells.get(2).value);
    }

    // For issue #742
    // https://github.com/OpenRefine/OpenRefine/issues/742
    @Test
    public void testFillDownRows() throws Exception {
        AbstractOperation op = new FillDownOperation(
                EngineConfig.reconstruct("{\"mode\":\"row-based\",\"facets\":[]}"),
                "second");
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();

        Assert.assertEquals("c", project.rows.get(0).cells.get(2).value);
        Assert.assertEquals("c", project.rows.get(1).cells.get(2).value);
        Assert.assertEquals("c", project.rows.get(2).cells.get(2).value);
        Assert.assertEquals("h", project.rows.get(3).cells.get(2).value);
    }

    @Test
    public void testKeyColumnIndex() throws Exception {
        // Shift all column indices
        for (Row r : project.rows) {
            r.cells.add(0, null);
        }
        List<Column> newColumns = new ArrayList<>();
        for (Column c : project.columnModel.columns) {
            newColumns.add(new Column(c.getCellIndex() + 1, c.getName()));
        }
        project.columnModel.columns.clear();
        project.columnModel.columns.addAll(newColumns);
        project.columnModel.update();

        AbstractOperation op = new FillDownOperation(
                EngineConfig.reconstruct("{\"mode\":\"record-based\",\"facets\":[]}"),
                "second");
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();

        Assert.assertEquals("c", project.rows.get(0).cells.get(3).value);
        Assert.assertEquals("c", project.rows.get(1).cells.get(3).value);
        Assert.assertNull(project.rows.get(2).cells.get(3));
        Assert.assertEquals("h", project.rows.get(3).cells.get(3).value);
    }
}
