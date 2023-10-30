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
import static org.testng.Assert.assertEquals;

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
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnInsertion;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.Grid;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ColumnRenameOperationTests extends RefineTest {

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
        OperationRegistry.registerOperation("core", "column-rename", ColumnRenameOperation.class);
    }

    @Test
    public void serializeColumnRenameOperation() throws Exception {
        String json = "{\"op\":\"core/column-rename\","
                + "  \"description\":\"Rename column old name to new name\","
                + "  \"oldColumnName\":\"old name\","
                + "  \"newColumnName\":\"new name\","
                + "  \"columnDependencies\" : [ ],"
                + "  \"columnInsertions\" : [ {"
                + "    \"copiedFrom\" : \"old name\","
                + "    \"insertAt\" : \"old name\","
                + "    \"name\" : \"new name\","
                + "    \"replace\" : true"
                + "  } ]"
                + "}";
        Operation op = ParsingUtilities.mapper.readValue(json, Operation.class);
        TestUtils.isSerializedTo(op, json, ParsingUtilities.defaultWriter);
    }

    @Test
    public void testRename() throws OperationException, ParsingException {
        Operation SUT = new ColumnRenameOperation("foo", "newfoo");
        ChangeResult changeResult = SUT.apply(initialState, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);

        List<IndexedRow> rows = changeResult.getGrid().collectRows();
        Assert.assertEquals(changeResult.getGrid().getColumnModel().getColumns(),
                Arrays.asList(
                        new ColumnMetadata("foo", "newfoo", 0L, null),
                        new ColumnMetadata("bar"),
                        new ColumnMetadata("hello")));
        Assert.assertEquals(rows.get(0).getRow().getCells(),
                Arrays.asList(new Cell("v1", null), new Cell("a", null), new Cell("d", null)));
    }

    @Test(expectedExceptions = OperationException.class)
    public void testNameConflict() throws OperationException, ParsingException {
        Operation SUT = new ColumnRenameOperation("foo", "bar");
        SUT.apply(initialState, mock(ChangeContext.class));
    }

    @Test
    public void testColumnarMetadata() {
        ColumnRenameOperation SUT = new ColumnRenameOperation("foo", "newfoo");

        assertEquals(SUT.getColumnDependencies(), Collections.emptyList());
        assertEquals(SUT.getColumnInsertions(), Arrays.asList(ColumnInsertion.builder()
                .withName("newfoo")
                .withInsertAt("foo")
                .withCopiedFrom("foo")
                .withReplace(true)
                .build()));
    }
}
