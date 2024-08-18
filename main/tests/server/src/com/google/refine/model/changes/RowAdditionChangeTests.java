/*******************************************************************************
 * Copyright (C) 2024, OpenRefine contributors
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

package com.google.refine.model.changes;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.RefineTest;
import com.google.refine.history.Change;
import com.google.refine.model.Cell;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.Pool;

public class RowAdditionChangeTests extends RefineTest {

    Project project;
    int originalCount;
    int insertionIndex;
    RowAdditionChange change;
    List<Row> newRows;
    Serializable[][] grid = {
            { "Electronics", 599.9 },
            { "Clothing", 45.50 },
            { "Home & Garden", 129.95 }
    };

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void SetUp() throws IOException, ModelException {
        insertionIndex = 0; // Prepend rows
        String[] columnNames = { "Category", "Value" };
        project = createProject(columnNames, grid);
        originalCount = project.rows.size();

        newRows = new ArrayList<>();
        newRows.add(new Row(0)); // Blank row
        newRows.add(new Row(0)); // Blank row

        change = new RowAdditionChange(newRows, insertionIndex);
    }

    @AfterMethod
    public void tearDown() {
        ProjectManager.singleton.deleteProject(project.id);
    }

    @Test
    // After prepend apply, row count equals sum of original rows and new rows
    public void testPrependApplyRowCount() {
        assert project.rows.size() == originalCount;
        change.apply(project);
        int actual = project.rows.size();
        int expected = originalCount + newRows.size();
        assertEquals(actual, expected);
    }

    @Test
    // After prepend apply, project's new prepended rows are identical those passed to constructor
    public void testPrependApplyRowIdentity() {
        change.apply(project);
        for (int i = insertionIndex; i < newRows.size(); i++) {
            Row actual = project.rows.get(i);
            Row expected = newRows.get(i);
            assertSame(actual, expected);
        }
    }

    @Test
    // After prepend apply, project's new rows are blank
    public void testPrependApplyCellLength() {
        change.apply(project);
        for (int i = insertionIndex; i < newRows.size(); i++) {
            Row row = project.rows.get(i);
            assertBlankRow(row);
        }
    }

    @Test
    // After prepend apply, existing rows are shifted up by the size of additional rows
    public void testPrependApplyExistingCellValues() {
        change.apply(project);
        for (int i = 0; i < grid.length; i++) {
            Row row = project.rows.get(i + newRows.size());
            for (int j = 0; j < row.cells.size(); j++) {
                Cell cell = row.getCell(j);
                assertEquals(grid[i][j], cell.value);
            }
        }
    }

    @Test
    // After prepend revert, row count equals original row count
    public void testPrependRevertRowCount() {
        assert project.rows.size() == originalCount;
        change.apply(project);
        change.revert(project);
        int actual = project.rows.size();
        int expected = originalCount;
        assertEquals(actual, expected);
    }

    @Test
    // After prepend revert, cell values equals original values
    public void testPrependRevertCellValue() {
        change.apply(project);
        change.revert(project);
        for (int i = 0; i < project.rows.size(); i++) {
            Row row = project.rows.get(i);
            for (int j = 0; j < row.cells.size(); j++) {
                Cell cell = row.cells.get(j);
                assertEquals(cell.value, grid[i][j]);
            }
        }
    }

    @Test
    // After prepend apply, loaded change produces blank rows
    public void testPrependLoadChangeBlankRows() throws Exception {
        Change change = loadChange("changes/blank_row_addition.txt");
        int changeCount = 2; // Value in file
        int changeIndex = 0; // Value in file
        change.apply(project);
        for (int i = changeIndex; i < insertionIndex + changeCount; i++) {
            Row row = project.rows.get(i);
            assertBlankRow(row);
        }
    }

    @Test
    // After prepend save, serialization behaves as expected
    public void testPrependSaveSerialization() throws IOException {
        Writer writer = new StringWriter();
        change.save(writer, new Properties());
        String expected = "index=0\n" +
                "count=2\n" +
                "{\"starred\":false,\"flagged\":false,\"cells\":[]}\n" +
                "{\"starred\":false,\"flagged\":false,\"cells\":[]}\n" +
                "/ec/\n";
        assertEquals(writer.toString(), expected);
    }

    @Test
    // After append apply, row count equals sum of original rows and new rows
    public void testAppendApplyRowCount() {
        assert project.rows.size() == originalCount;
        insertionIndex = project.rows.size();
        change = new RowAdditionChange(newRows, insertionIndex);
        change.apply(project);
        int actual = project.rows.size();
        int expected = originalCount + newRows.size();
        assertEquals(actual, expected);
    }

    @Test
    // After append apply, project's new append rows are identical those passed to constructor
    public void testAppendApplyRowIdentity() {
        insertionIndex = project.rows.size();
        change = new RowAdditionChange(newRows, insertionIndex);
        change.apply(project);

        for (int i = insertionIndex; i < newRows.size(); i++) {
            Row actual = project.rows.get(insertionIndex + i);
            Row expected = newRows.get(i);
            assertSame(actual, expected);
        }
    }

    @Test
    // After append revert, row count equals original row count
    public void testAppendRevertCellValue() {
        insertionIndex = project.rows.size();
        change = new RowAdditionChange(newRows, insertionIndex);
        change.apply(project);
        change.revert(project);

        for (int i = 0; i < project.rows.size(); i++) {
            Row row = project.rows.get(i);
            for (int j = 0; j < row.cells.size(); j++) {
                Cell cell = row.cells.get(j);
                assertEquals(cell.value, grid[i][j]);
            }
        }
    }

    /**
     * Loads a saved changed from disk
     * 
     * @param path:
     *            path to the saved change
     * @return Change: the saved changed
     * @throws Exception
     */
    private Change loadChange(String path) throws Exception {
        Pool pool = new Pool();
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream(path);
        assert in != null;
        LineNumberReader lineReader = new LineNumberReader(new InputStreamReader(in));
        // skip the header
        lineReader.readLine();
        lineReader.readLine();
        return RowAdditionChange.load(lineReader, pool);
    }

    /**
     * Assert that a row is blank. Where blank is defined by the following assertions: - `row.cells` is not null -
     * `row.cells` is empty
     * 
     * @param row
     */
    private void assertBlankRow(Row row) {
        assertNotNull(row.cells);
        assertTrue(row.cells.isEmpty());
    }
}
