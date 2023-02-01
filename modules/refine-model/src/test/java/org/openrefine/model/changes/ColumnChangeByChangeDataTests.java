
package org.openrefine.model.changes;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.model.Cell;
import org.openrefine.model.Row;
import org.openrefine.model.changes.ColumnChangeByChangeData.Joiner;

public class ColumnChangeByChangeDataTests {

    @Test
    public void testJoinerAdd() {
        Joiner joiner = new Joiner(2, true);
        Row row = joiner.call(4, new Row(Arrays.asList(new Cell(1, null), new Cell(2, null))), new Cell(3, null));
        Assert.assertEquals(row, new Row(Arrays.asList(new Cell(1, null), new Cell(2, null), new Cell(3, null))));
    }

    @Test
    public void testJoinerReplace() {
        Joiner joiner = new Joiner(0, false);
        Row row = joiner.call(4, new Row(Arrays.asList(new Cell(1, null), new Cell(2, null))), new Cell(3, null));
        Assert.assertEquals(row, new Row(Arrays.asList(new Cell(3, null), new Cell(2, null))));
    }

    @Test
    public void testJoinerReplaceNull() {
        // this behaviour is important to make sure reconciling only a subset of a column does not blank out
        // the cells outside the subset
        Joiner joiner = new Joiner(0, false);
        Row row = joiner.call(4, new Row(Arrays.asList(new Cell(1, null), new Cell(2, null))), null);
        Assert.assertEquals(row, new Row(Arrays.asList(new Cell(1, null), new Cell(2, null))));
    }
}
