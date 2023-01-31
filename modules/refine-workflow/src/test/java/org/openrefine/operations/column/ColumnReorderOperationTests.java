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
import java.util.List;

import org.openrefine.RefineTest;
import org.openrefine.expr.EvalError;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.grel.Parser;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.Grid;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

public class ColumnReorderOperationTests extends RefineTest {

    protected Grid initialState;

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
        OperationRegistry.registerOperation("core", "column-reorder", ColumnReorderOperation.class);
    }

    @Test
    public void serializeColumnReorderOperation() {
        Operation op = new ColumnReorderOperation(Arrays.asList("b", "c", "a"));
        TestUtils.isSerializedTo(op, "{\"op\":\"core/column-reorder\","
                + "\"description\":\"Reorder columns\","
                + "\"columnNames\":[\"b\",\"c\",\"a\"]}", ParsingUtilities.defaultWriter);
    }

    @Test
    public void testReorder() throws DoesNotApplyException, ParsingException {
        Change SUT = new ColumnReorderOperation(Arrays.asList("hello", "bar")).createChange();
        Change.ChangeResult changeResult = SUT.apply(initialState, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_ROWS);
        Grid applied = changeResult.getGrid();

        List<IndexedRow> rows = applied.collectRows();
        Assert.assertEquals(applied.getColumnModel().getColumns(),
                Arrays.asList(new ColumnMetadata("hello"), new ColumnMetadata("bar")));
        Assert.assertEquals(rows.get(0).getRow().getCells(),
                Arrays.asList(new Cell("d", null), new Cell("a", null)));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testDuplicateFinalNames() throws ParsingException {
        new ColumnReorderOperation(Arrays.asList("bar", "bar"));
    }

    @Test(expectedExceptions = Change.DoesNotApplyException.class)
    public void testDoesNotExist() throws DoesNotApplyException, ParsingException {
        Change SUT = new ColumnReorderOperation(Arrays.asList("does_not_exist", "bar")).createChange();
        SUT.apply(initialState, mock(ChangeContext.class));
    }

}
