
package org.openrefine.operations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.ColumnInsertion;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.Cell;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.RowMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.exceptions.DuplicateColumnException;
import org.openrefine.operations.exceptions.MissingColumnException;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.overlay.OverlayModel;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class RowMapOperationTests {

    long historyEntryId = 1234L;
    ChangeContext changeContext;

    Grid initialGrid;
    Grid mappedGrid;

    ColumnModel initialColumnModel;
    ColumnModel mappedColumnModel;

    GenericRowMapOperation SUT = new GenericRowMapOperation();

    public static class GenericRowMapOperation extends RowMapOperation {

        protected GenericRowMapOperation() {
            super(EngineConfig.ALL_ROWS);
        }

        @Override
        public String getDescription() {
            return "some funny operation doing a lot of things on the grid";
        }

        @Override
        public List<String> getColumnDependencies() {
            return Arrays.asList("A", "C");
        }

        @Override
        public Set<String> getColumnDeletions() {
            return Collections.singleton("C");
        }

        @Override
        public List<ColumnInsertion> getColumnInsertions() {
            return Arrays.asList(
                    new ColumnInsertion("new B", "D", false, "B"),
                    new ColumnInsertion("E", "A", false, null),
                    new ColumnInsertion("overwritten B", "B", true, null),
                    new ColumnInsertion("D", "D", true, "A"));
        }

        @Override
        protected RowInRecordMapper getPositiveRowMapper(ColumnModel columnModel, Map<String, OverlayModel> overlayModels,
                ChangeContext context) throws OperationException {
            return new RowInRecordMapper() {

                private static final long serialVersionUID = 4306016663853788347L;

                @Override
                public Row call(Record record, long rowId, Row row) {
                    return new Row(Arrays.asList(
                            new Cell(Objects.toString(row.getCellValue(1)), null),
                            new Cell(Objects.toString(row.getCellValue(0)), null)));
                }

                @Override
                public boolean preservesRecordStructure() {
                    return true;
                }

            };
        }

        @Override
        protected RowInRecordMapper getNegativeRowMapper(ColumnModel columnModel, Map<String, OverlayModel> overlayModels,
                ChangeContext context) throws OperationException {
            return new RowInRecordMapper() {

                private static final long serialVersionUID = 4306016663853788347L;

                @Override
                public Row call(Record record, long rowId, Row row) {
                    return new Row(Arrays.asList(null, null));
                }

                @Override
                public boolean preservesRecordStructure() {
                    return true;
                }

            };
        }
    }

    @BeforeTest
    public void setUp() {
        changeContext = mock(ChangeContext.class);
        when(changeContext.getHistoryEntryId()).thenReturn(historyEntryId);

        initialGrid = mock(Grid.class);
        mappedGrid = mock(Grid.class);

        initialColumnModel = new ColumnModel(Arrays.asList(
                new ColumnMetadata("A"),
                new ColumnMetadata("B"),
                new ColumnMetadata("C"),
                new ColumnMetadata("D")));
        when(initialGrid.getColumnModel()).thenReturn(initialColumnModel);

        mappedColumnModel = new ColumnModel(Arrays.asList(
                new ColumnMetadata("A", "A", 0L, null),
                new ColumnMetadata("A", "E", 1234L, null),
                new ColumnMetadata("C", "overwritten B", 1234L, null),
                new ColumnMetadata("A", "D", 0L, null),
                new ColumnMetadata("B", "new B", 0L, null)));
        when(mappedGrid.getColumnModel()).thenReturn(mappedColumnModel);

        RowMapper anyRowMapper = any();
        when(initialGrid.mapRows(anyRowMapper, eq(mappedColumnModel))).thenReturn(mappedGrid);
        when(mappedGrid.withOverlayModels(any())).thenReturn(mappedGrid);
    }

    @Test
    public void testApply() throws OperationException {
        ChangeResult result = SUT.apply(initialGrid, changeContext);

        verify(initialGrid, times(1)).mapRows(any(RowMapper.class), eq(mappedColumnModel));
        assertEquals(result.getGrid(), mappedGrid);
    }

    @Test
    public void testMissingDependency() {
        ColumnModel columnModel = new ColumnModel(Arrays.asList(
                new ColumnMetadata("A"),
                new ColumnMetadata("B"),
                new ColumnMetadata("D")));
        Grid grid = mock(Grid.class);
        when(grid.getColumnModel()).thenReturn(columnModel);

        assertThrows(MissingColumnException.class, () -> SUT.apply(grid, changeContext));
    }

    @Test
    public void testMissingCopiedColumn() {
        ColumnModel columnModel = new ColumnModel(Arrays.asList(
                new ColumnMetadata("A"),
                new ColumnMetadata("C"),
                new ColumnMetadata("D")));
        Grid grid = mock(Grid.class);
        when(grid.getColumnModel()).thenReturn(columnModel);

        assertThrows(MissingColumnException.class, () -> SUT.apply(grid, changeContext));
    }

    @Test
    public void testDuplicateColumnReplaced() {
        ColumnModel columnModel = new ColumnModel(Arrays.asList(
                new ColumnMetadata("A"),
                new ColumnMetadata("B"),
                new ColumnMetadata("C"),
                new ColumnMetadata("D"),
                new ColumnMetadata("E")));

        Grid grid = mock(Grid.class);
        when(grid.getColumnModel()).thenReturn(columnModel);

        assertThrows(DuplicateColumnException.class, () -> SUT.apply(grid, changeContext));
    }

    @Test
    public void testDuplicateColumnAdded() {
        ColumnModel columnModel = new ColumnModel(Arrays.asList(
                new ColumnMetadata("A"),
                new ColumnMetadata("B"),
                new ColumnMetadata("C"),
                new ColumnMetadata("D"),
                new ColumnMetadata("overwritten B")));

        Grid grid = mock(Grid.class);
        when(grid.getColumnModel()).thenReturn(columnModel);

        assertThrows(DuplicateColumnException.class, () -> SUT.apply(grid, changeContext));
    }

}
