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

package com.google.refine.operations.row;

import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine.Mode;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.process.Process;
import com.google.refine.sorting.SortingConfig;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class RowReorderOperationTests extends RefineTest {

    Project project = null;

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "row-reorder", RowReorderOperation.class);
    }

    @BeforeMethod
    public void setUp() {
        project = createCSVProject(
                "key,first\n" +
                        "8,b\n" +
                        ",d\n" +
                        "2,f\n" +
                        "1,h\n");
    }

    @AfterMethod
    public void tearDown() {
        ProjectManager.singleton.deleteProject(project.id);
    }

    @Test
    public void testSortEmptyString() throws Exception {
        String sortingJson = "{\"criteria\":[{\"column\":\"key\",\"valueType\":\"number\",\"reverse\":false,\"blankPosition\":2,\"errorPosition\":1}]}";
        SortingConfig sortingConfig = SortingConfig.reconstruct(sortingJson);
        project.rows.get(1).cells.set(0, new Cell("", null));
        AbstractOperation op = new RowReorderOperation(
                Mode.RowBased, sortingConfig);
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();

        Assert.assertEquals("h", project.rows.get(0).cells.get(1).value);
        Assert.assertEquals("f", project.rows.get(1).cells.get(1).value);
        Assert.assertEquals("b", project.rows.get(2).cells.get(1).value);
        Assert.assertEquals("d", project.rows.get(3).cells.get(1).value);
    }

    @Test
    public void serializeRowReorderOperation() throws Exception {
        String json = "  {\n" +
                "    \"op\": \"core/row-reorder\",\n" +
                "    \"description\": \"Reorder rows\",\n" +
                "    \"mode\": \"record-based\",\n" +
                "    \"sorting\": {\n" +
                "      \"criteria\": [\n" +
                "        {\n" +
                "          \"errorPosition\": 1,\n" +
                "          \"valueType\": \"number\",\n" +
                "          \"column\": \"start_year\",\n" +
                "          \"blankPosition\": 2,\n" +
                "          \"reverse\": false\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, RowReorderOperation.class), json);
    }

}
