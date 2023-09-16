
package org.openrefine.model.changes;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.EvalError;
import org.openrefine.history.Change;
import org.openrefine.history.Change.DoesNotApplyException;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.model.changes.ColumnSplitChange.Mode;

public class ColumnSplitChangeTests extends ColumnChangeTestBase {

    GridState toSplit;

    @BeforeTest
    public void createSplitProject() {
        Project project = createProject("my split project", new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a,b,c", "aBcDe", "d12t" },
                        { ",,d", "aFHiJ", "f34t" },
                        { ",,,,", "aT23L", "g18te" },
                        { 12, "b", "h" },
                        { new EvalError("error"), "a", "" },
                        { "12,true", "b", "g1" }
                });
        toSplit = project.getCurrentGridState();
    }

    @Test(expectedExceptions = Change.DoesNotApplyException.class)
    public void testDoesNotExist() throws DoesNotApplyException {
        Change SUT = new ColumnSplitChange("does_not_exist", Mode.Lengths, null, null, null, new int[] { 1, 2 }, EngineConfig.ALL_ROWS,
                false, false);
        SUT.apply(initialState);
    }

    @Test
    public void testSeparator() throws DoesNotApplyException {
        Change SUT = new ColumnSplitChange("foo", Mode.Separator, ",", false, null, null, EngineConfig.ALL_ROWS, false, false);
        GridState result = SUT.apply(toSplit);

        List<String> columnNames = result.getColumnModel().getColumns().stream().map(c -> c.getName()).collect(Collectors.toList());
        Assert.assertEquals(columnNames, Arrays.asList("foo", "foo 1", "foo 2", "foo 3", "foo 4", "foo 5", "bar", "hello"));
        List<Row> rows = result.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows.get(0).getCellValue(0), "a,b,c");
        Assert.assertEquals(rows.get(0).getCellValue(1), "a");
        Assert.assertEquals(rows.get(0).getCellValue(2), "b");
        Assert.assertEquals(rows.get(0).getCellValue(3), "c");
        Assert.assertEquals(rows.get(0).getCellValue(4), null);
        Assert.assertEquals(rows.get(0).getCellValue(5), null);
        Assert.assertEquals(rows.get(0).getCellValue(6), "aBcDe");
        Assert.assertEquals(rows.get(1).getCellValue(0), ",,d");
        Assert.assertEquals(rows.get(1).getCellValue(1), "");
        Assert.assertEquals(rows.get(1).getCellValue(2), "");
        Assert.assertEquals(rows.get(1).getCellValue(3), "d");
        Assert.assertEquals(rows.get(1).getCellValue(4), null);
        Assert.assertEquals(rows.get(1).getCellValue(5), null);
        Assert.assertEquals(rows.get(1).getCellValue(6), "aFHiJ");
        Assert.assertEquals(rows.get(3).getCellValue(0), 12);
        Assert.assertEquals(rows.get(3).getCellValue(1), null);
        Assert.assertEquals(rows.get(3).getCellValue(2), null);
        Assert.assertEquals(rows.get(3).getCellValue(3), null);
        Assert.assertEquals(rows.get(3).getCellValue(4), null);
        Assert.assertEquals(rows.get(3).getCellValue(5), null);
        Assert.assertEquals(rows.get(3).getCellValue(6), "b");
    }

    @Test
    public void testSeparatorMaxColumns() throws DoesNotApplyException {
        Change SUT = new ColumnSplitChange("foo", Mode.Separator, ",", false, 2, null, EngineConfig.ALL_ROWS, false, false);
        GridState result = SUT.apply(toSplit);

        List<String> columnNames = result.getColumnModel().getColumns().stream().map(c -> c.getName()).collect(Collectors.toList());
        Assert.assertEquals(columnNames, Arrays.asList("foo", "foo 1", "foo 2", "bar", "hello"));
        List<Row> rows = result.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows.get(0).getCellValue(0), "a,b,c");
        Assert.assertEquals(rows.get(0).getCellValue(1), "a");
        Assert.assertEquals(rows.get(0).getCellValue(2), "b");
        Assert.assertEquals(rows.get(0).getCellValue(3), "aBcDe");
        Assert.assertEquals(rows.get(1).getCellValue(0), ",,d");
        Assert.assertEquals(rows.get(1).getCellValue(1), "");
        Assert.assertEquals(rows.get(1).getCellValue(2), "");
        Assert.assertEquals(rows.get(1).getCellValue(3), "aFHiJ");
        Assert.assertEquals(rows.get(3).getCellValue(0), 12);
        Assert.assertEquals(rows.get(3).getCellValue(1), null);
        Assert.assertEquals(rows.get(3).getCellValue(2), null);
        Assert.assertEquals(rows.get(3).getCellValue(3), "b");
    }

    @Test
    public void testSeparatorDetectType() throws DoesNotApplyException {
        Change SUT = new ColumnSplitChange("foo", Mode.Separator, ",", false, 2, null, EngineConfig.ALL_ROWS, false, true);
        GridState result = SUT.apply(toSplit);

        List<String> columnNames = result.getColumnModel().getColumns().stream().map(c -> c.getName()).collect(Collectors.toList());
        Assert.assertEquals(columnNames, Arrays.asList("foo", "foo 1", "foo 2", "bar", "hello"));
        List<Row> rows = result.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows.get(5).getCellValue(0), "12,true");
        Assert.assertEquals(rows.get(5).getCellValue(1), 12L);
        Assert.assertEquals(rows.get(5).getCellValue(2), "true"); // we currently only parse numbers, curiously
        Assert.assertEquals(rows.get(5).getCellValue(3), "b");
    }

    @Test
    public void testSeparatorRemoveColumn() throws DoesNotApplyException {
        Change SUT = new ColumnSplitChange("foo", Mode.Separator, ",", false, 2, null, EngineConfig.ALL_ROWS, true, true);
        GridState result = SUT.apply(toSplit);

        List<String> columnNames = result.getColumnModel().getColumns().stream().map(c -> c.getName()).collect(Collectors.toList());
        Assert.assertEquals(columnNames, Arrays.asList("foo 1", "foo 2", "bar", "hello"));
        List<Row> rows = result.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows.get(5).getCellValue(0), 12L);
        Assert.assertEquals(rows.get(5).getCellValue(1), "true"); // we currently only parse numbers, curiously
        Assert.assertEquals(rows.get(5).getCellValue(2), "b");
    }

    @Test
    public void testRegex() throws DoesNotApplyException {
        Change SUT = new ColumnSplitChange("bar", Mode.Separator, "[A-Z]", true, null, null, EngineConfig.ALL_ROWS, false, false);
        GridState result = SUT.apply(toSplit);

        List<String> columnNames = result.getColumnModel().getColumns().stream().map(c -> c.getName()).collect(Collectors.toList());
        Assert.assertEquals(columnNames, Arrays.asList("foo", "bar", "bar 1", "bar 2", "bar 3", "hello"));
        List<Row> rows = result.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows.get(0).getCellValue(0), "a,b,c");
        Assert.assertEquals(rows.get(0).getCellValue(1), "aBcDe");
        Assert.assertEquals(rows.get(0).getCellValue(2), "a");
        Assert.assertEquals(rows.get(0).getCellValue(3), "c");
        Assert.assertEquals(rows.get(0).getCellValue(4), "e");
        Assert.assertEquals(rows.get(0).getCellValue(5), "d12t");
        Assert.assertEquals(rows.get(1).getCellValue(0), ",,d");
        Assert.assertEquals(rows.get(1).getCellValue(1), "aFHiJ");
        Assert.assertEquals(rows.get(1).getCellValue(2), "a");
        Assert.assertEquals(rows.get(1).getCellValue(3), "");
        Assert.assertEquals(rows.get(1).getCellValue(4), "i");
        Assert.assertEquals(rows.get(1).getCellValue(5), "f34t");
    }

    @Test
    public void testLengths() throws DoesNotApplyException {
        Change SUT = new ColumnSplitChange("hello", Mode.Lengths, null, false, null, new int[] { 1, 2 }, EngineConfig.ALL_ROWS, false,
                false);
        GridState result = SUT.apply(toSplit);

        List<String> columnNames = result.getColumnModel().getColumns().stream().map(c -> c.getName()).collect(Collectors.toList());
        Assert.assertEquals(columnNames, Arrays.asList("foo", "bar", "hello", "hello 1", "hello 2"));
        List<Row> rows = result.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows.get(0).getCellValue(0), "a,b,c");
        Assert.assertEquals(rows.get(0).getCellValue(1), "aBcDe");
        Assert.assertEquals(rows.get(0).getCellValue(2), "d12t");
        Assert.assertEquals(rows.get(0).getCellValue(3), "d");
        Assert.assertEquals(rows.get(0).getCellValue(4), "12");
        Assert.assertEquals(rows.get(1).getCellValue(0), ",,d");
        Assert.assertEquals(rows.get(1).getCellValue(1), "aFHiJ");
        Assert.assertEquals(rows.get(1).getCellValue(2), "f34t");
        Assert.assertEquals(rows.get(1).getCellValue(3), "f");
        Assert.assertEquals(rows.get(1).getCellValue(4), "34");
    }

    @Test
    public void testRespectsFacets() throws DoesNotApplyException {
        ColumnSplitChange SUT = new ColumnSplitChange("foo", Mode.Separator, ",", false, null, null, EngineConfig.ALL_ROWS, false, false);

        Engine engine = mock(Engine.class);
        when(engine.getMode()).thenReturn(Engine.Mode.RowBased);
        when(engine.combinedRowFilters()).thenReturn(new OddRowFilter());
        when(engine.aggregateFilteredRows(any(), any())).thenReturn(3);
        ColumnSplitChange spied = spy(SUT);
        when(spied.getEngine(any())).thenReturn(engine);

        GridState result = spied.apply(toSplit);
        List<Row> rows = result.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows.get(0).getCellValue(0), "a,b,c");
        Assert.assertEquals(rows.get(0).getCellValue(1), null);
        Assert.assertEquals(rows.get(0).getCellValue(2), null);
        Assert.assertEquals(rows.get(0).getCellValue(3), null);
        Assert.assertEquals(rows.get(0).getCellValue(4), "aBcDe");
        Assert.assertEquals(rows.get(1).getCellValue(0), ",,d");
        Assert.assertEquals(rows.get(1).getCellValue(1), "");
        Assert.assertEquals(rows.get(1).getCellValue(2), "");
        Assert.assertEquals(rows.get(1).getCellValue(3), "d");
    }

    protected static class OddRowFilter implements RowFilter {

        private static final long serialVersionUID = -4875472935974784439L;

        @Override
        public boolean filterRow(long rowIndex, Row row) {
            return rowIndex % 2L == 1;
        }

    }
}
