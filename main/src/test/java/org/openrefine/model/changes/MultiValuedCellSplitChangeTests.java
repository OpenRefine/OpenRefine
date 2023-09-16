
package org.openrefine.model.changes;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.history.Change;
import org.openrefine.history.Change.DoesNotApplyException;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Row;
import org.openrefine.model.changes.ColumnSplitChange.Mode;

public class MultiValuedCellSplitChangeTests extends RefineTest {

    GridState initialState;
    GridState smallGrid;

    @BeforeTest
    public void setUpGrid() {
        initialState = createGrid(
                new String[] { "key", "foo", "bar" },
                new Serializable[][] {
                        { "record1", "a||b", "c" },
                        { null, "c|d", "e" },
                        { null, 12, "f" },
                        { "record2", "", "g" },
                        { null, "h|i", "" },
                        { null, null, "j" },
                        { null, null, null }
                });

        smallGrid = createGrid(
                new String[] { "Key", "Value" },
                new Serializable[][] {
                        { "Record_1", "one:two;three four" } });
    }

    @Test(expectedExceptions = DoesNotApplyException.class)
    public void testInvalidColumn() throws DoesNotApplyException {
        Change SUT = new MultiValuedCellSplitChange("does_not_exist", Mode.Separator, ",", false, null);
        SUT.apply(initialState);
    }

    @Test
    public void testSplit() throws DoesNotApplyException {
        Change SUT = new MultiValuedCellSplitChange("foo", Mode.Separator, "|", false, null);
        GridState applied = SUT.apply(initialState);

        GridState expectedState = createGrid(
                new String[] { "key", "foo", "bar" },
                new Serializable[][] {
                        { "record1", "a", "c" },
                        { null, "", null },
                        { null, "b", null },
                        { null, "c", "e" },
                        { null, "d", null },
                        { null, 12, "f" },
                        { "record2", "", "g" },
                        { null, "h", "" },
                        { null, "i", "j" },
                        { null, null, null }
                });

        Assert.assertEquals(applied.getColumnModel(), initialState.getColumnModel());
        List<IndexedRow> rows = applied.collectRows();
        Assert.assertEquals(rows, expectedState.collectRows());
    }

    /**
     * Test to demonstrate the intended behaviour of the function, for issue #1268
     * https://github.com/OpenRefine/OpenRefine/issues/1268
     */

    @Test
    public void testSplitMultiValuedCellsTextSeparator() throws Exception {
        Change change = new MultiValuedCellSplitChange(
                "Value",
                Mode.Separator,
                ":",
                false,
                null);
        GridState applied = change.apply(smallGrid);

        List<IndexedRow> rows = applied.collectRows();

        Assert.assertEquals(rows.get(0).getRow().getCellValue(0), "Record_1");
        Assert.assertEquals(rows.get(0).getRow().getCellValue(1), "one");
        Assert.assertEquals(rows.get(1).getRow().getCellValue(0), null);
        Assert.assertEquals(rows.get(1).getRow().getCellValue(1), "two;three four");
    }

    @Test
    public void testSplitMultiValuedCellsRegExSeparator() throws Exception {
        Change change = new MultiValuedCellSplitChange(
                "Value",
                Mode.Separator,
                "\\W",
                true,
                null);
        GridState applied = change.apply(smallGrid);

        List<Row> rows = applied.collectRows().stream().map(r -> r.getRow()).collect(Collectors.toList());

        Assert.assertEquals(rows.get(0).getCellValue(0), "Record_1");
        Assert.assertEquals(rows.get(0).getCellValue(1), "one");
        Assert.assertEquals(rows.get(1).getCellValue(0), null);
        Assert.assertEquals(rows.get(1).getCellValue(1), "two");
        Assert.assertEquals(rows.get(2).getCellValue(0), null);
        Assert.assertEquals(rows.get(2).getCellValue(1), "three");
        Assert.assertEquals(rows.get(3).getCellValue(0), null);
        Assert.assertEquals(rows.get(3).getCellValue(1), "four");
    }

    @Test
    public void testSplitMultiValuedCellsLengths() throws Exception {
        int[] lengths = { 4, 4, 6, 4 };

        Change change = new MultiValuedCellSplitChange(
                "Value",
                Mode.Lengths,
                null,
                false,
                lengths);

        GridState applied = change.apply(smallGrid);

        List<Row> rows = applied.collectRows().stream().map(r -> r.getRow()).collect(Collectors.toList());

        Assert.assertEquals(rows.get(0).getCellValue(0), "Record_1");
        Assert.assertEquals(rows.get(0).getCellValue(1), "one:");
        Assert.assertEquals(rows.get(1).getCellValue(0), null);
        Assert.assertEquals(rows.get(1).getCellValue(1), "two;");
        Assert.assertEquals(rows.get(2).getCellValue(0), null);
        Assert.assertEquals(rows.get(2).getCellValue(1), "three ");
        Assert.assertEquals(rows.get(3).getCellValue(0), null);
        Assert.assertEquals(rows.get(3).getCellValue(1), "four");
    }

}
