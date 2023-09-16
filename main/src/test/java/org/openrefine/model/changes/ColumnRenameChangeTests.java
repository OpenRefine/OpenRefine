
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

public class ColumnRenameChangeTests extends ColumnChangeTestBase {

    @Test
    public void testRename() throws DoesNotApplyException {
        Change SUT = new ColumnRenameChange("foo", "newfoo");
        GridState applied = SUT.apply(initialState);

        List<IndexedRow> rows = applied.collectRows();
        Assert.assertEquals(applied.getColumnModel().getColumns(),
                Arrays.asList(new ColumnMetadata("foo", "newfoo", null, null), new ColumnMetadata("bar"), new ColumnMetadata("hello")));
        Assert.assertEquals(rows.get(0).getRow().getCells(),
                Arrays.asList(new Cell("v1", null), new Cell("a", null), new Cell("d", null)));
    }

    @Test(expectedExceptions = DoesNotApplyException.class)
    public void testNameConflict() throws DoesNotApplyException {
        Change SUT = new ColumnRenameChange("foo", "bar");
        SUT.apply(initialState);
    }
}
