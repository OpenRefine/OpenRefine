/*******************************************************************************
 * Copyright (C) 2018,2023 OpenRefine contributors
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

package com.google.refine.expr.functions;

import static org.testng.Assert.assertEquals;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.HasFieldsListImpl;
import com.google.refine.expr.WrappedCell;
import com.google.refine.expr.WrappedRow;
import com.google.refine.grel.GrelTestBase;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

/**
 * Test cases for cross function.
 */
public class CrossTests extends GrelTestBase {

    private static final String ERROR_MSG = "cross expects a cell or value, a project name to look up (optional), and a column name in that project (optional)";
    private static final OffsetDateTime dateTimeValue = OffsetDateTime.parse("2017-05-12T05:45:00+00:00",
            DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    private HasFieldsListImpl emptyList;

    // dependencies
    Project projectGift;
    Project projectAddress;
    Project projectDuplicate1;
    Project projectDuplicate2;

    // data from: https://github.com/OpenRefine/OpenRefine/wiki/GREL-Other-Functions
    @BeforeMethod
    public void SetUp() {
        bindings = new Properties();
        emptyList = new HasFieldsListImpl();

        projectAddress = createProject("My Address Book",
                new String[] { "friend", "address" },
                new Serializable[][] {
                        { "john", "120 Main St." },
                        { "mary", "50 Broadway Ave." },
                        { "john", "999 XXXXXX St." },
                        { "anne", "17 Morning Crescent" },
                        { dateTimeValue, "dateTime" },
                        { 1600, "integer" },
                        { 123456789123456789L, "long" },
                        { true, "boolean" },
                        { 3.14, "double" },
                });

        projectGift = createProject("Christmas Gifts",
                new String[] { "gift", "recipient" },
                new Serializable[][] {
                        { "lamp", "mary" },
                        { "clock", "john" },
                        { "dateTime", dateTimeValue },
                        { "integer", 1600 },
                        { "123456789123456789", 123456789123456789L },
                        { "boolean", true }
                });

        projectDuplicate1 = createProject("Duplicate",
                new String[] { "Col1", "Col2" },
                new Serializable[][] {});
        projectDuplicate2 = createProject("Duplicate",
                new String[] { "Col1", "Col2" },
                new Serializable[][] {});

        bindings.put("project", projectGift);
        // add a column address based on column recipient
        bindings.put("columnName", "recipient");
    }

    @Test
    public void crossFunctionMissingProject() throws Exception {
        String nonExistentProject = "NOPROJECT";
        assertEquals(((EvalError) invoke("cross", "Anne", nonExistentProject, "friend")).message,
                "Unable to find project with name: " + nonExistentProject);
    }

    @Test
    public void crossFunctionMultipleProjects() throws Exception {
        String duplicateProjectName = "Duplicate";
        assertEquals(((EvalError) invoke("cross", "Anne", duplicateProjectName, "friend")).message,
                "Multiple (2) projects found with name: " + duplicateProjectName);
    }

    @Test
    public void crossFunctionMissingColumn() throws Exception {
        String nonExistentColumn = "NoColumn";
        String projectName = "My Address Book";
        assertEquals(((EvalError) invoke("cross", "mary", projectName, nonExistentColumn)).message,
                "Unable to find column " + nonExistentColumn + " in project " + projectName);
    }

    @Test
    public void crossFunctionSameColumnTest() throws Exception {
        Project project = (Project) bindings.get("project");
        Cell c = project.rows.get(0).cells.get(1);
        WrappedCell lookup = new WrappedCell(project, "recipient", c);
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", lookup, "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "50 Broadway Ave.");
    }

    /**
     * The result shouldn't depend on the based column in "bindings" when the first argument is a WrappedCell instance.
     */
    @Test
    public void crossFunctionDifferentColumnTest() throws Exception {
        Project project = (Project) bindings.get("project");
        bindings.put("columnName", "gift"); // change the based column
        Cell c = project.rows.get(0).cells.get(1);
        WrappedCell lookup = new WrappedCell(project, "recipient", c);
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", lookup, "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "50 Broadway Ave.");
    }

    @Test
    // lookup the row with index 0 in the current project
    public void crossFunctionOneArgumentTest() throws Exception {
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", 0)).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "mary");
    }

    @Test
    public void crossFunctionOneArgumentTest1() throws Exception {
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", 0, "")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "mary");
    }

    @Test
    public void crossFunctionOneArgumentTest2() throws Exception {
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", 1, "", "")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "john");
    }

    @Test
    public void crossFunctionTwoArgumentTest() throws Exception {
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", "lamp", "", "gift")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "mary");
    }

    @Test
    public void crossFunctionTwoArgumentTest1() throws Exception {
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", 0, "My Address Book")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "120 Main St.");
    }

    @Test
    public void crossFunctionTwoArgumentTest2() throws Exception {
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", 0, "My Address Book", "")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "120 Main St.");
    }

    @Test
    public void crossFunctionOneToOneTest() throws Exception {
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", "mary", "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "50 Broadway Ave.");
    }

    /**
     * To demonstrate that the cross function can look up multiple rows.
     */
    @Test
    public void crossFunctionOneToManyTest() throws Exception {
        List<WrappedRow> rows = (((List<WrappedRow>) invoke("cross", "john", "My Address Book", "friend")));
        assertEquals(rows.size(), 2);
        Row row = rows.get(1).row;
        String address = row.getCell(1).value.toString();
        assertEquals(address, "999 XXXXXX St.");
    }

    /**
     * Check that record field accessor works for a list of WrappedRecords.
     *
     * (Technically not a cross() test, but this is pretty much the only place they're used)
     */
    @Test
    public void crossFunctionRecordFieldLookup() throws Exception {
        String tests[][] = {
                { "'john'.cross('My Address Book', 'friend').cells[0]['friend'].value", "john" },
                { "'john'.cross('My Address Book', 'friend').index.toString()", "[0, 2]" },
                { "'john'.cross('My Address Book', 'friend').columnNames[0].toString()", "[friend, address]" },
                // Make sure delegation from WrappedRow to Row works
                { "'john'.cross('My Address Book', 'friend').flagged.toString()", "[false, false]" },
                { "'john'.cross('My Address Book', 'friend').starred.toString()", "[false, false]" },
                // Unknown field names return null value rather than an error
                { "'john'.cross('My Address Book', 'friend').foo.toString()", "[null, null]" },
                // TODO: I'm not sure what record.cells is supposed to return. Returning single valued lists seems weird
                { "'john'.cross('My Address Book', 'friend').record.cells['friend'].value.toString()", "[[john], [john]]" },
                { "'john'.cross('My Address Book', 'friend').record.index.toString()", "[0, 2]" },
                { "'john'.cross('My Address Book', 'friend').record.fromRowIndex.toString()", "[0, 2]" },
                { "'john'.cross('My Address Book', 'friend').record.toRowIndex.toString()", "[1, 3]" },
                { "'john'.cross('My Address Book', 'friend').record.rowCount.toString()", "[1, 1]" },
                // Below returns null on error intentionally
                { "'john'.cross('My Address Book', 'friend').record.foo.toString()", "[null, null]" },
        };
        for (String[] test : tests) {
            parseEval(bindings, test);
        }
    }

    @Test
    public void crossFunctionCaseSensitiveTest() throws Exception {
        Assert.assertEquals(invoke("cross", "Anne", "My Address Book", "friend"), emptyList);
    }

    @Test
    public void crossFunctionDateTimeTest() throws Exception {
        Project project = (Project) bindings.get("project");
        Cell c = project.rows.get(2).cells.get(1);
        WrappedCell lookup = new WrappedCell(project, "recipient", c);
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", lookup, "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "dateTime");
    }

    @Test
    public void crossFunctionIntegerTest() throws Exception {
        Project project = (Project) bindings.get("project");
        Cell c = project.rows.get(3).cells.get(1);
        WrappedCell lookup = new WrappedCell(project, "recipient", c);
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", lookup, "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "integer");
    }

    @Test
    public void crossFunctionBooleanTest() throws Exception {
        Project project = (Project) bindings.get("project");
        Cell c = project.rows.get(5).cells.get(1);
        WrappedCell lookup = new WrappedCell(project, "recipient", c);
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", lookup, "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "boolean");
    }

    @Test
    public void crossFunctionIntegerArgumentTest() throws Exception {
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", 1600, "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "integer");
    }

    @Test
    public void crossFunctionIntegerArgumentTest1() throws Exception {
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", 1600L, "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "integer");
    }

    @Test
    public void crossFunctionIntegerArgumentTest2() throws Exception {
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", "1600", "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "integer");
    }

    /**
     * Two values will match if and only if they have the same string representation. In this case, "1600.0" doesn't
     * equal to "1600".
     */

    @Test
    public void crossFunctionIntegerArgumentTest3() throws Exception {
        Assert.assertEquals(invoke("cross", "1600.0", "My Address Book", "friend"), emptyList);
    }

    @Test
    public void crossFunctionLongArgumentTest() throws Exception {
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", 123456789123456789L, "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "long");
    }

    @Test
    public void crossFunctionLongArgumentTest1() throws Exception {
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", "123456789123456789", "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "long");
    }

    @Test
    public void crossFunctionDoubleArgumentTest() throws Exception {
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", 3.14, "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "double");
    }

    @Test
    public void crossFunctionDoubleArgumentTest1() throws Exception {
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", 3.14f, "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "double");
    }

    @Test
    public void crossFunctionDoubleArgumentTest2() throws Exception {
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", "3.14", "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "double");
    }

    @Test
    public void crossFunctionDateTimeArgumentTest() throws Exception {
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", dateTimeValue, "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "dateTime");
    }

    @Test
    public void crossFunctionDateTimeArgumentTest1() throws Exception {
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", dateTimeValue.toString(), "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "dateTime");
    }

    @Test
    public void crossFunctionBooleanArgumentTest() throws Exception {
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", true, "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "boolean");
    }

    @Test
    public void crossFunctionBooleanArgumentTest1() throws Exception {
        Row row = (((WrappedRow) ((HasFieldsListImpl) invoke("cross", "true", "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        assertEquals(address, "boolean");
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
        Assert.assertEquals(invoke("cross", "NON-EXIST", "My Address Book", "friend"), emptyList);
    }

    /**
     * 
     * rest of cells shows "Error: cross expects a cell or cell value, a project name to look up, and a column name in
     * that project"
     */
    @Test
    public void crossFunctionNonLiteralValue() throws Exception {
        assertEquals(((EvalError) invoke("cross", null, "My Address Book", "friend")).message, ERROR_MSG);
    }

    @Test
    public void crossFunctionNullParams() throws Exception {
        assertEquals(((EvalError) invoke("cross", "dummy", null, null)).message, ERROR_MSG);
        assertEquals(((EvalError) invoke("cross", "dummy", null)).message, ERROR_MSG);
        assertEquals(((EvalError) invoke("cross", "dummy", null, "test")).message, ERROR_MSG);
        assertEquals(((EvalError) invoke("cross", "dummy", 1.0, 1)).message, ERROR_MSG);
    }

}
