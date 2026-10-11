/*******************************************************************************
 * Copyright (C) 2026, OpenRefine contributors
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

import java.io.LineNumberReader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.history.Change;
import com.google.refine.history.History;
import com.google.refine.model.Project;
import com.google.refine.util.Pool;

public class ColumnNameSerializationTests extends RefineTest {

    @DataProvider
    public Object[][] columnNames() {
        return new Object[][] {
                { "first\nsecond" },
                { "first\rsecond" },
                { "first\r\nsecond" },
                { "first\n/ec/\nnewColumnName=wrong" },
                { "literal\\n\\r\\t\"quoted\"" },
                { "ordinary=column" },
                { "\"quoted\"" },
                { "null" }
        };
    }

    @Test(dataProvider = "columnNames")
    public void renameRoundTrip(String name) throws Exception {
        ColumnRenameChange original = new ColumnRenameChange(name, "new " + name);
        StringWriter writer = new StringWriter();
        original.save(writer, new Properties());
        ColumnRenameChange loaded = (ColumnRenameChange) ColumnRenameChange.load(
                new LineNumberReader(new StringReader(writer.toString())), new Pool());
        assertEquals(loaded._oldColumnName, name);
        assertEquals(loaded._newColumnName, "new " + name);
    }

    @DataProvider
    public Object[][] changes() {
        List<Object[]> cases = new ArrayList<>();
        for (Object[] names : columnNames()) {
            String name = (String) names[0];
            List<Change> changes = List.of(
                    new ColumnAdditionChange("added " + name, 1, Collections.emptyList()),
                    new ColumnMoveChange(name, 1),
                    new ColumnReorderChange(List.of("second", name)),
                    new ColumnSplitChange(name, List.of("split " + name), Collections.emptyList(), Collections.emptyList(), false),
                    new MassCellChange(Collections.emptyList(), name, false),
                    new DataExtensionChange(name, "https://example.com/reconcile", "ids", "schema", 1,
                            List.of("extended " + name), Collections.singletonList(null), Collections.emptyList(),
                            Collections.emptyList(), 1));
            for (Change change : changes) {
                cases.add(new Object[] { change, name });
            }
        }
        return cases.toArray(new Object[0][]);
    }

    @Test(dataProvider = "changes")
    public void allColumnChangesRoundTrip(Change original, String name) throws Exception {
        Project project = createProject(new String[] { name, "second" }, new Serializable[0][0]);
        original.apply(project);
        StringWriter writer = new StringWriter();
        History.writeOneChange(writer, original, new Pool());
        Change loaded = History.readOneChange(new LineNumberReader(new StringReader(writer.toString())), new Pool());
        StringWriter reserialized = new StringWriter();
        History.writeOneChange(reserialized, loaded, new Pool());
        assertEquals(reserialized.toString(), writer.toString(), original.getClass().getSimpleName());
        loaded.revert(project);
        assertEquals(project.columnModel.getColumnNames(), List.of(name, "second"));
        loaded.apply(project);
    }

    @Test(dataProvider = "columnNames")
    public void renameUndoAfterReload(String name) throws Exception {
        Project project = createProject(new String[] { name }, new Serializable[0][0]);
        ColumnRenameChange change = new ColumnRenameChange(name, "new " + name);
        change.apply(project);
        StringWriter writer = new StringWriter();
        change.save(writer, new Properties());
        Change loaded = ColumnRenameChange.load(new LineNumberReader(new StringReader(writer.toString())), new Pool());
        loaded.revert(project);
        assertEquals(project.columnModel.getColumnNames(), List.of(name));
        loaded.apply(project);
        assertEquals(project.columnModel.getColumnNames(), List.of("new " + name));
    }

    @DataProvider
    public Object[][] legacyNames() {
        return new Object[][] {
                { "literal\\n\\r\\t\"quoted\"" },
                { "\"quoted\"" },
                { "null" },
                { "{\"name\":\"value\"}" }
        };
    }

    @Test(dataProvider = "legacyNames")
    public void legacyNamesAreNotUnescaped(String name) throws Exception {
        String rename = "oldColumnName=" + name + "\nnewColumnName=new " + name + "\n/ec/\n";
        ColumnRenameChange loaded = (ColumnRenameChange) ColumnRenameChange.load(
                new LineNumberReader(new StringReader(rename)), new Pool());
        assertEquals(loaded._oldColumnName, name);
        assertEquals(loaded._newColumnName, "new " + name);

        String[] scalarClasses = { "ColumnAdditionChange", "ColumnMoveChange", "ColumnSplitChange" };
        for (String cls : scalarClasses) {
            String data = "columnName=" + name + "\nnewCellCount=0\n"
                    + (cls.equals("ColumnSplitChange") ? "columnNameCount=1\n" + name + "\n" : "") + "/ec/\n";
            Change change = History.readOneChange(new LineNumberReader(new StringReader(
                    "3.9.3\ncom.google.refine.model.changes." + cls + "\n" + data)), new Pool());
            String loadedName = switch (change) {
                case ColumnAdditionChange added -> added._columnName;
                case ColumnMoveChange moved -> moved._columnName;
                case ColumnSplitChange split -> split._columnName;
                default -> throw new AssertionError(cls);
            };
            assertEquals(loadedName, name);
        }
        ColumnReorderChange reordered = (ColumnReorderChange) ColumnReorderChange.load(
                new LineNumberReader(new StringReader("columnNameCount=1\n" + name + "\n/ec/\n")), new Pool());
        assertEquals(reordered._columnNames, List.of(name));
        MassCellChange cells = (MassCellChange) MassCellChange.load(
                new LineNumberReader(new StringReader("commonColumnName=" + name + "\ncellChangeCount=0\n/ec/\n")), new Pool());
        assertEquals(cells._commonColumnName, name);
        DataExtensionChange extended = (DataExtensionChange) DataExtensionChange.load(
                new LineNumberReader(new StringReader("baseColumnName=" + name + "\ncolumnNameCount=1\n" + name + "\n/ec/\n")), new Pool());
        assertEquals(extended._baseColumnName, name);
        assertEquals(extended._columnNames, List.of(name));
    }

}
