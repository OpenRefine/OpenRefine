
package org.openrefine.model.changes;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.history.Change;
import org.openrefine.history.Change.DoesNotApplyException;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;

public class KeyValueColumnizeChangeTests extends RefineTest {

    /**
     * Test in the case where an ID is available in the first column.
     * 
     * @throws Exception
     */
    @Test
    public void testKeyValueColumnizeWithID() throws Exception {
        GridState grid = createGrid(
                new String[] { "ID", "Cat", "Val" },
                new Serializable[][] {
                        { "1", "a", "1" },
                        { "1", "b", "3" },
                        { "2", "b", "4" },
                        { "2", "c", "5" },
                        { "3", "a", "2" },
                        { "3", "b", "5" },
                        { "3", "d", "3" } });

        Change change = new KeyValueColumnizeChange(
                "Cat", "Val", null);
        GridState applied = change.apply(grid);

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
        GridState grid = createGrid(
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

        Change change = new KeyValueColumnizeChange(
                "Key",
                "Value",
                null);
        GridState applied = change.apply(grid);

        ColumnModel columnModel = applied.getColumnModel();
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
        GridState grid = createGrid(
                new String[] { "Key", "Value", "Notes" },
                new Serializable[][] {
                        { "merchant", "Katie", "ref" },
                        { "fruit", "apple", "catalogue" },
                        { "price", "1.2", "pricelist" },
                        { "merchant", "John", "knowledge" },
                        { "fruit", "banana", "survey" },
                        { "price", "3.1", "legislation" }
                });

        Change change = new KeyValueColumnizeChange(
                "Key",
                "Value",
                "Notes");
        GridState applied = change.apply(grid);

        ColumnModel columnModel = applied.getColumnModel();
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
    public void testCopyRowsWithNoKeys() throws DoesNotApplyException {
        // when a key cell is empty, if there are other columns around, we simply copy those
        GridState grid = createGrid(
                new String[] { "Key", "Value" },
                new Serializable[][] {
                        { "merchant", "Katie" },
                        { "fruit", "apple" },
                        { "price", "1.2", },
                        { null, "John", },
                        { "fruit", "banana" },
                        { "price", "3.1", }
                });

        Change change = new KeyValueColumnizeChange(
                "Key",
                "Value",
                null);
        GridState applied = change.apply(grid);

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
