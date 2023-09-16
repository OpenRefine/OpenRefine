
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

public class ColumnRemovalChangeTests extends ColumnChangeTestBase {

    @Test
    public void testRemoval() throws DoesNotApplyException {
        Change SUT = new ColumnRemovalChange("foo");
        GridState applied = SUT.apply(initialState);
        List<IndexedRow> rows = applied.collectRows();
        Assert.assertEquals(applied.getColumnModel().getColumns(),
                Arrays.asList(new ColumnMetadata("bar"), new ColumnMetadata("hello")));
        Assert.assertEquals(rows.get(0).getRow().getCells(),
                Arrays.asList(new Cell("a", null), new Cell("d", null)));
    }

    @Test(expectedExceptions = DoesNotApplyException.class)
    public void testColumnNotFound() throws DoesNotApplyException {
        Change SUT = new ColumnRemovalChange("not_found");
        SUT.apply(initialState);
    }
}
