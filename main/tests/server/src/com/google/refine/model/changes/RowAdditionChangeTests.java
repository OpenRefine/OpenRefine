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
import static org.testng.Assert.assertSame;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.model.Cell;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class RowAdditionChangeTests extends RefineTest {

    Project project;
    int originalCount;
    int insertionIndex = 0; // Prepend rows
    RowAdditionChange change;
    List<Row> newRows;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void SetUp() throws IOException, ModelException {
        String[] columnNames = { "Category", "Value" };
        Serializable[][] grid = {
                { "Electronics", 599.9 },
                { "Clothing", 45.50 },
                { "Home & Garden", 129.95 }
        };
        project = createProject(columnNames, grid);
        originalCount = project.rows.size();

        newRows = new ArrayList<>();
        newRows.add(new Row(2));
        newRows.get(0).setCell(0, new Cell("Books", null));
        newRows.get(0).setCell(1, new Cell(19.99, null));
        newRows.add(new Row(2));
        newRows.get(1).setCell(0, new Cell("Sports & Outdoors", null));
        newRows.get(1).setCell(1, new Cell(89.99, null));

        change = new RowAdditionChange(newRows, insertionIndex);
    }

    @Test
    public void testApplyProjectRowCount() {
        change.apply(project);
        int actual = project.rows.size();
        int expected = originalCount + newRows.size();
        assertEquals(actual, expected);
    }

    @Test
    public void testRevertProjectRowCount() {
        change.apply(project);
        change.revert(project);
        int actual = project.rows.size();
        int expected = originalCount;
        assertEquals(actual, expected);
    }

    @Test
    public void testNewRowsIdentity() {
        change.apply(project);
        for (int i = insertionIndex; i < newRows.size(); i++) {
            Row actual = project.rows.get(i);
            Row expected = newRows.get(i);
            assertSame(actual, expected);
        }

    }
}
