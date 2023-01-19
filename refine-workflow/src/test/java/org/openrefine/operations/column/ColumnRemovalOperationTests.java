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

package org.openrefine.operations.column;

import static org.mockito.Mockito.mock;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.expr.EvalError;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.grel.Parser;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ColumnRemovalOperationTests extends RefineTest {

    protected GridState initialState;

    @BeforeMethod
    public void setUpInitialState() {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        initialState = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "v1", "a", "d" },
                        { "v3", "a", "f" },
                        { "", "a", "g" },
                        { "", "b", "h" },
                        { new EvalError("error"), "a", "i" },
                        { "v1", "b", "j" }
                });
    }

    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation("core", "column-removal", ColumnRemovalOperation.class);
    }

    @Test
    public void serializeColumnRemovalOperation() throws Exception {
        String json = "{\"op\":\"core/column-removal\","
                + "\"description\":\"Remove columns my column 1, my column 2\","
                + "\"columnNames\":[\"my column 1\", \"my column 2\"]}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnRemovalOperation.class), json,
                ParsingUtilities.defaultWriter);
    }

    @Test
    public void serializeColumnRemovalOperationOldFormat() throws Exception {
        String json = "{\"op\":\"core/column-removal\","
                + "\"description\":\"Remove column my column\","
                + "\"columnName\":\"my column\"}";
        String normalized = "{\"op\":\"core/column-removal\","
                + "\"description\":\"Remove column my column\","
                + "\"columnNames\":[\"my column\"]}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnRemovalOperation.class), normalized,
                ParsingUtilities.defaultWriter);
    }

    @Test
    public void testRemoval() throws DoesNotApplyException, ParsingException {
        Change SUT = new ColumnRemovalOperation(Collections.singletonList("foo")).createChange();
        GridState applied = SUT.apply(initialState, mock(ChangeContext.class));
        List<IndexedRow> rows = applied.collectRows();
        Assert.assertEquals(applied.getColumnModel().getColumns(),
                Arrays.asList(new ColumnMetadata("bar"), new ColumnMetadata("hello")));
        Assert.assertEquals(rows.get(0).getRow().getCells(),
                Arrays.asList(new Cell("a", null), new Cell("d", null)));
    }

    @Test
    public void testMultipleColumns() throws DoesNotApplyException {
        Change SUT = new ColumnRemovalOperation(Arrays.asList("foo", "bar")).createChange();
        GridState applied = SUT.apply(initialState, mock(ChangeContext.class));
        List<IndexedRow> rows = applied.collectRows();
        Assert.assertEquals(applied.getColumnModel().getColumns(),
                Arrays.asList(new ColumnMetadata("hello")));
        Assert.assertEquals(rows.get(0).getRow().getCells(),
                Arrays.asList(new Cell("d", null)));
    }

    @Test(expectedExceptions = DoesNotApplyException.class)
    public void testColumnNotFound() throws DoesNotApplyException, ParsingException {
        Change SUT = new ColumnRemovalOperation(Collections.singletonList("not_found")).createChange();
        SUT.apply(initialState, mock(ChangeContext.class));
    }
}
