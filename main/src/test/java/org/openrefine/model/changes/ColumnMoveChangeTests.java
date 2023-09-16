
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
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ColumnMoveChangeTests extends ColumnChangeTestBase {

    private GridState initialState;
    private String serializedChange = "{\n" +
            "  \"columnName\" : \"foo\",\n" +
            "  \"index\" : 1,\n" +
            "  \"type\" : \"org.openrefine.model.changes.ColumnMoveChange\"\n" +
            "}";

    @Test
    public void testForward() throws DoesNotApplyException {
        Change SUT = new ColumnMoveChange("foo", 1);
        GridState applied = SUT.apply(initialState);
        List<IndexedRow> rows = applied.collectRows();
        Assert.assertEquals(applied.getColumnModel().getColumns(),
                Arrays.asList(new ColumnMetadata("bar"), new ColumnMetadata("foo"), new ColumnMetadata("hello")));
        Assert.assertEquals(rows.get(0).getRow().getCells(),
                Arrays.asList(new Cell("a", null), new Cell("v1", null), new Cell("d", null)));
    }

    @Test
    public void testSamePosition() throws DoesNotApplyException {
        Change SUT = new ColumnMoveChange("bar", 1);
        GridState applied = SUT.apply(initialState);
        List<IndexedRow> rows = applied.collectRows();
        Assert.assertEquals(applied.getColumnModel().getColumns(),
                Arrays.asList(new ColumnMetadata("foo"), new ColumnMetadata("bar"), new ColumnMetadata("hello")));
        Assert.assertEquals(rows.get(0).getRow().getCells(),
                Arrays.asList(new Cell("v1", null), new Cell("a", null), new Cell("d", null)));
    }

    @Test
    public void testBackward() throws DoesNotApplyException {
        Change SUT = new ColumnMoveChange("hello", 1);
        GridState applied = SUT.apply(initialState);
        List<IndexedRow> rows = applied.collectRows();
        Assert.assertEquals(applied.getColumnModel().getColumns(),
                Arrays.asList(new ColumnMetadata("foo"), new ColumnMetadata("hello"), new ColumnMetadata("bar")));
        Assert.assertEquals(rows.get(0).getRow().getCells(),
                Arrays.asList(new Cell("v1", null), new Cell("d", null), new Cell("a", null)));
    }

    @Test(expectedExceptions = DoesNotApplyException.class)
    public void testColumnDoesNotExist() throws DoesNotApplyException {
        Change SUT = new ColumnMoveChange("not_found", 1);
        SUT.apply(initialState);
    }

    @Test
    public void testSerialize() {
        Change SUT = new ColumnMoveChange("foo", 1);
        TestUtils.isSerializedTo(SUT, serializedChange, ParsingUtilities.defaultWriter);
    }
}
