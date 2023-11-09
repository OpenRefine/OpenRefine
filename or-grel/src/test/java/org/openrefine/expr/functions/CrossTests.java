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

package org.openrefine.expr.functions;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.expr.EvalError;
import org.openrefine.expr.HasFieldsListImpl;
import org.openrefine.expr.WrappedCell;
import org.openrefine.expr.WrappedRow;
import org.openrefine.model.Cell;
import org.openrefine.model.ModelException;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

/**
 * Test cases for cross function.
 */
public class CrossTests extends FunctionTestBase {

    private static OffsetDateTime dateTimeValue = OffsetDateTime.parse("2017-05-12T05:45:00+00:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    // dependencies
    Project projectGift;
    Project projectAddress;
    Project projectDuplicate1;
    Project projectDuplicate2;

    // data from: https://github.com/OpenRefine/OpenRefine/wiki/GREL-Other-Functions
    @BeforeMethod
    public void SetUp() throws ModelException {
        projectAddress = createProject("My Address Book",
                new String[] { "friend", "address" },
                new Serializable[] {
                        "john", "120 Main St.",
                        "mary", "50 Broadway Ave.",
                        "john", "999 XXXXXX St.", // john's 2nd address
                        "anne", "17 Morning Crescent",
                        dateTimeValue, "dateTime",
                        1600, "integer",
                        true, "boolean" });

        projectGift = createProject("Christmas Gifts",
                new String[] { "gift", "recipient" },
                new Serializable[] {
                        "lamp", "mary",
                        "clock", "john",
                        "dateTime", dateTimeValue,
                        "integer", 1600,
                        "boolean", true });

        projectDuplicate1 = createProject("Duplicate", new String[] { "Col1", "Col2" }, new Serializable[] {});
        projectDuplicate2 = createProject("Duplicate", new String[] { "Col1", "Col2" }, new Serializable[] {});

        bindings.put("project", projectGift);

        // Add some non-string value cells to each project
        projectAddress.rows.get(4).cells.set(0, new Cell(dateTimeValue, null));
        projectAddress.rows.get(5).cells.set(0, new Cell(1600, null));
        projectAddress.rows.get(6).cells.set(0, new Cell(true, null));
        projectGift.rows.get(2).cells.set(1, new Cell(dateTimeValue, null));
        projectGift.rows.get(3).cells.set(1, new Cell(1600, null));
        projectGift.rows.get(4).cells.set(1, new Cell(true, null));

        // add a column address based on column recipient
        bindings.put("columnName", "recipient");
    }

    @Test
    public void crossFunctionMissingProject() throws Exception {
        String nonExistentProject = "NOPROJECT";
        Assert.assertEquals(((EvalError) invoke("cross", "Anne", nonExistentProject, "friend")).message,
                "Unable to find project with name: " + nonExistentProject);
    }

    @Test
    public void crossFunctionMultipleProjects() throws Exception {
        String duplicateProjectName = "Duplicate";
        Assert.assertEquals(((EvalError) invoke("cross", "Anne", duplicateProjectName, "friend")).message,
                "2 projects found with name: " + duplicateProjectName);
    }

    @Test
    public void crossFunctionMissingColumn() throws Exception {
        String nonExistentColumn = "NoColumn";
        String projectName = "My Address Book";
        Assert.assertEquals(((EvalError) invoke("cross", "mary", projectName, nonExistentColumn)).message,
                "Unable to find column " + nonExistentColumn + " in project " + projectName);
    }

    @Test
    public void crossFunctionOneToOneTest() throws Exception {
        Row row = ((Row) ((WrappedRow) ((HasFieldsListImpl) invoke("cross", "mary", "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        Assert.assertEquals(address, "50 Broadway Ave.");
    }

    /**
     * To demonstrate that the cross function can join multiple rows.
     */
    @Test
    public void crossFunctionOneToManyTest() throws Exception {
        Row row = ((Row) ((WrappedRow) ((HasFieldsListImpl) invoke("cross", "john", "My Address Book", "friend")).get(1)).row);
        String address = row.getCell(1).value.toString();
        Assert.assertEquals(address, "999 XXXXXX St.");
    }

    @Test
    public void crossFunctionCaseSensitiveTest() throws Exception {
        Assert.assertNull(invoke("cross", "Anne", "My Address Book", "friend"));
    }

    @Test
    public void crossFunctionDateTimeTest() throws Exception {
        Project project = (Project) bindings.get("project");
        Cell c = project.rows.get(2).cells.get(1);
        WrappedCell lookup = new WrappedCell(project, "recipient", c);
        Row row = ((Row) ((WrappedRow) ((HasFieldsListImpl) invoke("cross", lookup, "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        Assert.assertEquals(address, "dateTime");
    }

    @Test
    public void crossFunctionIntegerTest() throws Exception {
        Project project = (Project) bindings.get("project");
        Cell c = project.rows.get(3).cells.get(1);
        WrappedCell lookup = new WrappedCell(project, "recipient", c);
        Row row = ((Row) ((WrappedRow) ((HasFieldsListImpl) invoke("cross", lookup, "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        Assert.assertEquals(address, "integer");
    }

    @Test
    public void crossFunctionBooleanTest() throws Exception {
        Project project = (Project) bindings.get("project");
        Cell c = project.rows.get(4).cells.get(1);
        WrappedCell lookup = new WrappedCell(project, "recipient", c);
        Row row = ((Row) ((WrappedRow) ((HasFieldsListImpl) invoke("cross", lookup, "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        Assert.assertEquals(address, "boolean");
    }

    /**
     * If no match, return null.
     * 
     * But if user still apply grel:value.cross("My Address Book", "friend")[0].cells["address"].value, from the
     * "Preview", the target cell shows "Error: java.lang.IndexOutOfBoundsException: Index: 0, Size: 0". It will still
     * end up with blank if the onError set so.
     */
    @Test
    public void crossFunctionMatchNotFoundTest() throws Exception {
        Assert.assertNull(invoke("cross", "NON-EXIST", "My Address Book", "friend"));
    }

    /**
     * 
     * rest of cells shows "Error: cross expects a string or cell, a project name to join with, and a column name in
     * that project"
     */
    @Test
    public void crossFunctionNonLiteralValue() throws Exception {
        Assert.assertEquals(((EvalError) invoke("cross", 1, "My Address Book", "friend")).message,
                "cross expects a string or cell, a project name to join with, and a column name in that project");

        Assert.assertEquals(((EvalError) invoke("cross", null, "My Address Book", "friend")).message,
                "cross expects a string or cell, a project name to join with, and a column name in that project");

        Assert.assertEquals(((EvalError) invoke("cross", Calendar.getInstance(), "My Address Book", "friend")).message,
                "cross expects a string or cell, a project name to join with, and a column name in that project");
    }

    @Test
    public void serializeCross() {
        String json = "{\"description\":\"join with another project by column\",\"params\":\"cell c or string value, string projectName, string columnName\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new Cross(), json, ParsingUtilities.defaultWriter);
    }
}
