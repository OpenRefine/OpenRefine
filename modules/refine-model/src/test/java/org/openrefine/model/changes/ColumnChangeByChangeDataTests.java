
package org.openrefine.model.changes;

import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.model.Cell;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.changes.ColumnChangeByChangeData.Joiner;

public class ColumnChangeByChangeDataTests {

    @Test
    public void testJoinerAdd() {
        Joiner joiner = new Joiner(2, true, true);
        Row row1 = new Row(Arrays.asList(new Cell(1, null), new Cell(2, null)));
        Row row = joiner.call(row1, new IndexedData<>(4, new Cell(3, null)));
        Assert.assertEquals(row, new Row(Arrays.asList(new Cell(1, null), new Cell(2, null), new Cell(3, null))));
    }

    @Test
    public void testJoinerAddPending() {
        Joiner joiner = new Joiner(2, true, true);
        Row row1 = new Row(Arrays.asList(new Cell(1, null), new Cell(2, null)));
        Row row = joiner.call(row1, new IndexedData<>(4));
        Assert.assertEquals(row, new Row(Arrays.asList(new Cell(1, null), new Cell(2, null), Cell.PENDING_NULL)));
    }

    @Test
    public void testJoinerReplace() {
        Joiner joiner = new Joiner(0, false, true);
        Row row1 = new Row(Arrays.asList(new Cell(1, null), new Cell(2, null)));
        Row row = joiner.call(row1, new IndexedData<>(4, new Cell(3, null)));
        Assert.assertEquals(row, new Row(Arrays.asList(new Cell(3, null), new Cell(2, null))));
    }

    @Test
    public void testJoinerReplacePending() {
        Joiner joiner = new Joiner(0, false, true);
        Row row1 = new Row(Arrays.asList(new Cell(1, null), new Cell(2, null)));
        Row row = joiner.call(row1, new IndexedData<>(4));
        Assert.assertEquals(row, new Row(Arrays.asList(new Cell(1, null, true), new Cell(2, null))));
    }

    @Test
    public void testJoinerReplaceNull() {
        // this behaviour is important to make sure reconciling only a subset of a column does not blank out
        // the cells outside the subset
        Joiner joiner = new Joiner(0, false, true);
        Row row1 = new Row(Arrays.asList(new Cell(1, null), new Cell(2, null)));
        Row row = joiner.call(row1, new IndexedData<>(4, null));
        Assert.assertEquals(row, new Row(Arrays.asList(new Cell(1, null), new Cell(2, null))));
    }

    @Test
    public void testJoinerReplacePendingInRecord() {
        Joiner joiner = new Joiner(1, false, true);
        Row row1 = new Row(Arrays.asList(new Cell(1, null), new Cell(2, null)));
        Row row2 = new Row(Arrays.asList(Cell.NULL, new Cell(4, null)));
        Record record = new Record(0L, Arrays.asList(row1, row2));

        List<Row> result = joiner.call(record, new IndexedData<>(0L));
        Assert.assertEquals(result, Arrays.asList(
                new Row(Arrays.asList(new Cell(1, null), new Cell(2, null, true))),
                new Row(Arrays.asList(Cell.NULL, new Cell(4, null, true)))));
    }

}
