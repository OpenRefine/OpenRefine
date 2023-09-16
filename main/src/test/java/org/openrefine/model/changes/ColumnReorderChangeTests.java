
package org.openrefine.model.changes;

import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.history.Change;
import org.openrefine.history.Change.DoesNotApplyException;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;

public class ColumnReorderChangeTests extends ColumnChangeTestBase {

    @Test
    public void testReorder() throws DoesNotApplyException {
        Change SUT = new ColumnReorderChange(Arrays.asList("hello", "bar"));
        GridState applied = SUT.apply(initialState);

        List<IndexedRow> rows = applied.collectRows();
        Assert.assertEquals(applied.getColumnModel().getColumns(),
                Arrays.asList(new ColumnMetadata("hello"), new ColumnMetadata("bar")));
        Assert.assertEquals(rows.get(0).getRow().getCells(),
                Arrays.asList(new Cell("d", null), new Cell("a", null)));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testDuplicateFinalNames() {
        new ColumnReorderChange(Arrays.asList("bar", "bar"));
    }

    @Test(expectedExceptions = Change.DoesNotApplyException.class)
    public void testDoesNotExist() throws DoesNotApplyException {
        Change SUT = new ColumnReorderChange(Arrays.asList("does_not_exist", "bar"));
        SUT.apply(initialState);
    }

}
