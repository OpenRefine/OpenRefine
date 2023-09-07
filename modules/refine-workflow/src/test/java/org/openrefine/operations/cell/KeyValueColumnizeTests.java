/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.operations.cell;

import static org.mockito.Mockito.mock;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.expr.ParsingException;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Row;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class KeyValueColumnizeTests extends RefineTest {

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "key-value-columnize", KeyValueColumnizeOperation.class);
    }

    @Test
    public void serializeKeyValueColumnizeOperation() throws Exception {
        String json = "{\"op\":\"core/key-value-columnize\","
                + "\"description\":\"Columnize by key column key column and value column value column\","
                + "\"keyColumnName\":\"key column\","
                + "\"valueColumnName\":\"value column\","
                + "\"noteColumnName\":null}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, KeyValueColumnizeOperation.class), json,
                ParsingUtilities.defaultWriter);

        String jsonFull = "{\"op\":\"core/key-value-columnize\","
                + "\"description\":\"Columnize by key column key column and value column value column with note column note column\","
                + "\"keyColumnName\":\"key column\","
                + "\"valueColumnName\":\"value column\","
                + "\"noteColumnName\":\"note column\"}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(jsonFull, KeyValueColumnizeOperation.class), jsonFull,
                ParsingUtilities.defaultWriter);
    }

    /**
     * Test in the case where an ID is available in the first column.
     * 
     * @throws Exception
     */
    @Test
    public void testKeyValueColumnizeWithID() throws Exception {
        Grid grid = createGrid(
                new String[] { "ID", "Cat", "Val" },
                new Serializable[][] {
                        { "1", "a", "1" },
                        { "1", "b", "3" },
                        { "2", "b", "4" },
                        { "2", "c", "5" },
                        { "3", "a", "2" },
                        { "3", "b", "5" },
                        { "3", "d", "3" } });

        Operation operation = new KeyValueColumnizeOperation(
                "Cat", "Val", null);
        ChangeResult changeResult = operation.apply(grid, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.NO_ROW_PRESERVATION);

        Grid applied = changeResult.getGrid();

        ColumnModel columnModel = applied.getColumnModel();
        Assert.assertEquals(columnModel.getColumns().size(), 5);
        Assert.assertEquals(columnModel.getColumns().get(0).getName(), "ID");
        Assert.assertEquals(columnModel.getColumns().get(1).getName(), "a");
        Assert.assertEquals(columnModel.getColumns().get(2).getName(), "b");
        Assert.assertEquals(columnModel.getColumns().get(3).getName(), "c");
        Assert.assertEquals(columnModel.getColumns().get(4).getName(), "d");

        List<Row> rows = applied.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows.size(), 3);

        Assert.assertEquals(rows.get(0).cells.get(0).value, "1");
        Assert.assertEquals(rows.get(0).cells.get(1).value, "1");
        Assert.assertEquals(rows.get(0).cells.get(2).value, "3");

        // 2,,4,5,
        Assert.assertEquals(rows.get(1).cells.get(0).value, "2");
        Assert.assertEquals(rows.get(1).cells.get(2).value, "4");
        Assert.assertEquals(rows.get(1).cells.get(3).value, "5");

        // 3,2,5,,3
        Assert.assertEquals(rows.get(2).cells.get(0).value, "3");
        Assert.assertEquals(rows.get(2).cells.get(1).value, "2");
        Assert.assertEquals(rows.get(2).cells.get(2).value, "5");
        Assert.assertEquals(rows.get(2).cells.get(3).value, null);
        Assert.assertEquals(rows.get(2).cells.get(4).value, "3");
    }

    /**
     * Test to demonstrate the intended behaviour of the function, for issue #1214
     * https://github.com/OpenRefine/OpenRefine/issues/1214
     */
    @Test
    public void testKeyValueColumnize() throws Exception {
        Grid grid = createGrid(
                new String[] { "Key", "Value" },
                new Serializable[][] {
                        { "merchant", "Katie" },
                        { "fruit", "apple" },
                        { "price", "1.2" },
                        { "fruit", "pear" },
                        { "price", "1.5" },
                        { "merchant", "John" },
                        { "fruit", "banana" },
                        { "price", "3.1" } });

        Operation operation = new KeyValueColumnizeOperation(
                "Key",
                "Value",
                null);
        ChangeResult changeResult = operation.apply(grid, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.NO_ROW_PRESERVATION);

        Grid applied = changeResult.getGrid();

        ColumnModel columnModel = applied.getColumnModel();
        Assert.assertTrue(columnModel.hasRecords());
        int merchantCol = columnModel.getColumnIndexByName("merchant");
        int fruitCol = columnModel.getColumnIndexByName("fruit");
        int priceCol = columnModel.getColumnIndexByName("price");

        List<Row> rows = applied.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows.size(), 3);
        Assert.assertEquals(rows.get(0).getCellValue(merchantCol), "Katie");
        Assert.assertEquals(rows.get(1).getCellValue(merchantCol), null);
        Assert.assertEquals(rows.get(2).getCellValue(merchantCol), "John");
        Assert.assertEquals(rows.get(0).getCellValue(fruitCol), "apple");
        Assert.assertEquals(rows.get(1).getCellValue(fruitCol), "pear");
        Assert.assertEquals(rows.get(2).getCellValue(fruitCol), "banana");
        Assert.assertEquals(rows.get(0).getCellValue(priceCol), "1.2");
        Assert.assertEquals(rows.get(1).getCellValue(priceCol), "1.5");
        Assert.assertEquals(rows.get(2).getCellValue(priceCol), "3.1");
    }

    @Test
    public void testKeyValueColumnizeNotes() throws Exception {
        Grid grid = createGrid(
                new String[] { "Key", "Value", "Notes" },
                new Serializable[][] {
                        { "merchant", "Katie", "ref" },
                        { "fruit", "apple", "catalogue" },
                        { "price", "1.2", "pricelist" },
                        { "merchant", "John", "knowledge" },
                        { "fruit", "banana", "survey" },
                        { "price", "3.1", "legislation" }
                });

        Operation operation = new KeyValueColumnizeOperation(
                "Key",
                "Value",
                "Notes");
        ChangeResult changeResult = operation.apply(grid, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.NO_ROW_PRESERVATION);

        Grid applied = changeResult.getGrid();

        ColumnModel columnModel = applied.getColumnModel();
        Assert.assertTrue(columnModel.hasRecords());
        Assert.assertEquals(columnModel.getColumnNames(),
                Arrays.asList("merchant", "fruit", "price", "Notes : merchant", "Notes : fruit", "Notes : price"));

        List<Row> rows = applied.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows.size(), 2);
        Assert.assertEquals(rows.get(0).getCellValue(0), "Katie");
        Assert.assertEquals(rows.get(1).getCellValue(0), "John");
        Assert.assertEquals(rows.get(0).getCellValue(1), "apple");
        Assert.assertEquals(rows.get(1).getCellValue(1), "banana");
        Assert.assertEquals(rows.get(0).getCellValue(2), "1.2");
        Assert.assertEquals(rows.get(1).getCellValue(2), "3.1");
        Assert.assertEquals(rows.get(0).getCellValue(3), "ref");
        Assert.assertEquals(rows.get(1).getCellValue(3), "knowledge");
        Assert.assertEquals(rows.get(0).getCellValue(4), "catalogue");
        Assert.assertEquals(rows.get(1).getCellValue(4), "survey");
        Assert.assertEquals(rows.get(0).getCellValue(5), "pricelist");
        Assert.assertEquals(rows.get(1).getCellValue(5), "legislation");
    }

    @Test
    public void testKeyValueColumnizeIdenticalValues() throws Exception {
        Grid grid = createGrid(
                new String[] { "Key", "Value", "wd" },
                new Serializable[][] {
                        { "merchant", "Katie", "34" },
                        { "fruit", "apple", "34" },
                        { "price", "1.2", "34" },
                        { "merchant", "John", "56" },
                        { "fruit", "banana", "56" },
                        { "price", "3.1", "56" }
                });

        Operation operation = new KeyValueColumnizeOperation(
                "Key",
                "Value",
                null);
        ChangeResult changeResult = operation.apply(grid, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.NO_ROW_PRESERVATION);

        Grid applied = changeResult.getGrid();

        Grid expected = createGridWithRecords(
                new String[] { "wd", "merchant", "fruit", "price" },
                new Serializable[][] {
                        { "34", "Katie", "apple", "1.2" },
                        { "56", "John", "banana", "3.1" }
                });

        assertGridEquals(applied, expected);
    }

    @Test
    public void testCopyRowsWithNoKeys() throws OperationException, ParsingException {
        // when a key cell is empty, if there are other columns around, we simply copy those
        Grid grid = createGrid(
                new String[] { "Key", "Value" },
                new Serializable[][] {
                        { "merchant", "Katie" },
                        { "fruit", "apple" },
                        { "price", "1.2", },
                        { null, "John", },
                        { "fruit", "banana" },
                        { "price", "3.1", }
                });

        Operation operation = new KeyValueColumnizeOperation(
                "Key",
                "Value",
                null);
        ChangeResult changeResult = operation.apply(grid, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.NO_ROW_PRESERVATION);

        Grid applied = changeResult.getGrid();

        ColumnModel columnModel = applied.getColumnModel();
        int merchantCol = columnModel.getColumnIndexByName("merchant");
        int fruitCol = columnModel.getColumnIndexByName("fruit");
        int priceCol = columnModel.getColumnIndexByName("price");

        List<Row> rows = applied.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows.size(), 2);
        Assert.assertEquals(rows.get(0).getCellValue(merchantCol), "Katie");
        Assert.assertEquals(rows.get(1).getCellValue(merchantCol), null);
        Assert.assertEquals(rows.get(0).getCellValue(fruitCol), "apple");
        Assert.assertEquals(rows.get(1).getCellValue(fruitCol), "banana");
        Assert.assertEquals(rows.get(0).getCellValue(priceCol), "1.2");
        Assert.assertEquals(rows.get(1).getCellValue(priceCol), "3.1");
    }

}
