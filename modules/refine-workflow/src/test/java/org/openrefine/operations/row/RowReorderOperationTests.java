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

package org.openrefine.operations.row;

import static org.mockito.Mockito.mock;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Grid;
import org.openrefine.model.Row;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.sorting.SortingConfig;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class RowReorderOperationTests extends RefineTest {

    Grid initial;

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "row-reorder", RowReorderOperation.class);
    }

    @BeforeMethod
    public void setUp() {
        initial = createGrid(
                new String[] { "key", "first" },
                new Serializable[][] {
                        { "8", "b" },
                        { "", "d" },
                        { "2", "f" },
                        { "1", "h" },
                        { "9", "F" },
                        { "10", "f" } });

    }

    @Test
    public void testSortEmptyString() throws Exception {
        String sortingJson = "{\"criteria\":[{\"column\":\"key\",\"valueType\":\"number\",\"reverse\":false,\"blankPosition\":2,\"errorPosition\":1}]}";
        SortingConfig sortingConfig = SortingConfig.reconstruct(sortingJson);

        Operation operation = new RowReorderOperation(Mode.RowBased, sortingConfig);
        ChangeResult changeResult = operation.apply(initial, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.NO_ROW_PRESERVATION);
        Grid applied = changeResult.getGrid();
        List<Row> rows = applied.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());

        Assert.assertEquals(rows.get(0).cells.get(1).value, "h");
        Assert.assertEquals(rows.get(1).cells.get(1).value, "f");
        Assert.assertEquals(rows.get(2).cells.get(1).value, "b");
        Assert.assertEquals(rows.get(3).cells.get(1).value, "F");
        Assert.assertEquals(rows.get(4).cells.get(1).value, "f");
        Assert.assertEquals(rows.get(5).cells.get(1).value, "d");
    }

    @Test
    public void testReverseSort() throws Exception {
        String sortingJson = "{\"criteria\":[{\"column\":\"key\",\"valueType\":\"number\",\"reverse\":true,\"blankPosition\":-1,\"errorPosition\":1}]}";
        SortingConfig sortingConfig = SortingConfig.reconstruct(sortingJson);
        Operation op = new RowReorderOperation(
                Mode.RowBased, sortingConfig);
        ChangeResult changeResult = op.apply(initial, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.NO_ROW_PRESERVATION);
        Grid applied = changeResult.getGrid();
        List<Row> rows = applied.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());

        Assert.assertEquals(rows.get(5).cells.get(1).value, "h");
        Assert.assertEquals(rows.get(4).cells.get(1).value, "f");
        Assert.assertEquals(rows.get(3).cells.get(1).value, "b");
        Assert.assertEquals(rows.get(2).cells.get(1).value, "F");
        Assert.assertEquals(rows.get(1).cells.get(1).value, "f");
        Assert.assertEquals(rows.get(0).cells.get(1).value, "d"); // controlled by blankPosition, not reverse
    }

    @Test
    public void testStringSort() throws Exception {
        String sortingJson = "{\"criteria\":[{\"column\":\"first\",\"valueType\":\"string\",\"reverse\":false,\"blankPosition\":2,\"errorPosition\":1,\"caseSensitive\":true}]}";
        SortingConfig sortingConfig = SortingConfig.reconstruct(sortingJson);
        Operation op = new RowReorderOperation(
                Mode.RowBased, sortingConfig);
        ChangeResult changeResult = op.apply(initial, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.NO_ROW_PRESERVATION);
        Grid applied = changeResult.getGrid();
        List<Row> rows = applied.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());

        Assert.assertEquals(rows.get(0).cells.get(1).value, "b");
        Assert.assertEquals(rows.get(1).cells.get(1).value, "d");
        Assert.assertEquals(rows.get(2).cells.get(1).value, "f");
        Assert.assertEquals(rows.get(3).cells.get(1).value, "f");
        Assert.assertEquals(rows.get(4).cells.get(1).value, "F");
        Assert.assertEquals(rows.get(5).cells.get(1).value, "h");
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
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, RowReorderOperation.class), json, ParsingUtilities.defaultWriter);
    }

}
