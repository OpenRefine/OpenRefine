
package org.openrefine.operations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.Facet;
import org.openrefine.browsing.facets.FacetConfig;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnId;
import org.openrefine.model.ColumnInsertion;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Record;
import org.openrefine.model.RecordFilter;
import org.openrefine.model.RecordMapper;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.RowMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.model.changes.RecordChangeDataJoiner;
import org.openrefine.model.changes.RowChangeDataJoiner;
import org.openrefine.model.changes.RowInRecordChangeDataJoiner;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.operations.exceptions.DuplicateColumnException;
import org.openrefine.operations.exceptions.MissingColumnException;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.overlay.OverlayModel;

public class RowMapOperationTests {

    long projectId = 6789L;
    long historyEntryId = 1234L;
    ChangeContext changeContext;

    Grid initialGrid;
    Grid rowMappedGrid;
    Grid recordMappedGrid;
    Grid rowJoinedGridNeutralEngine;
    Grid rowJoinedGridWithFacet;
    Grid recordJoinedGrid;
    Grid opaquelyMappedGrid;

    ChangeData<Row> rowMappedChangeData;
    ChangeData<List<Row>> recordMappedChangeData;

    ColumnModel initialColumnModel;
    ColumnModel mappedColumnModelLazy;
    ColumnModel mappedColumnModelEagerNeutralEngine;
    ColumnModel mappedColumnModelEagerWithFacet;
    ColumnModel mappedColumnModelOpaque;

    LazyRowMapOperation SUT_rowsLazy = new LazyRowMapOperation(EngineConfig.ALL_ROWS);
    LazyRowMapOperation SUT_recordsLazy = new LazyRowMapOperation(EngineConfig.ALL_RECORDS);
    EagerRowMapOperation SUT_rowsEager = new EagerRowMapOperation(EngineConfig.ALL_ROWS);
    EagerRowMapOperation SUT_rowsEagerWithFacet;
    EagerRowMapOperation SUT_recordsEager = new EagerRowMapOperation(EngineConfig.ALL_RECORDS);
    OpaqueRowMapOperation SUT_rowsOpaque = new OpaqueRowMapOperation(EngineConfig.ALL_ROWS);

    EngineConfig engineConfigWithFacet;

    static ReconConfig reconConfigA = mock(ReconConfig.class);
    static ReconConfig reconConfigB = mock(ReconConfig.class);

    private static class LazyRowMapOperation extends RowMapOperation {

        protected LazyRowMapOperation(EngineConfig config) {
            super(config);
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
                    new ColumnInsertion("new B", "D", false, "B", null, false),
                    new ColumnInsertion("E", "A", false, null, null, false),
                    new ColumnInsertion("overwritten B", "B", true, null, reconConfigB, true),
                    new ColumnInsertion("D", "D", true, "A", reconConfigB, false));
        }

        @Override
        protected RowInRecordMapper getPositiveRowMapper(ColumnModel columnModel, Map<String, OverlayModel> overlayModels,
                long estimatedRowCount, ChangeContext context) throws OperationException {
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
                long estimatedRowCount, ChangeContext context) throws OperationException {
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

    private static class EagerRowMapOperation extends RowMapOperation {

        protected EagerRowMapOperation(EngineConfig config) {
            super(config);
        }

        @Override
        public String getDescription() {
            return "some funny operation doing a lot of things on the grid and persisting them";
        }

        @Override
        public List<String> getColumnDependencies() {
            return Arrays.asList("A", "D");
        }

        @Override
        public Set<String> getColumnDeletions() {
            return Collections.singleton("C");
        }

        @Override
        public List<ColumnInsertion> getColumnInsertions() {
            return Arrays.asList(
                    new ColumnInsertion("new B", null, false, "B", null, false));
        }

        @Override
        protected RowInRecordMapper getPositiveRowMapper(ColumnModel columnModel, Map<String, OverlayModel> overlayModels,
                long estimatedRowCount, ChangeContext context) throws OperationException {
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

                @Override
                public boolean persistResults() {
                    return true;
                }

            };
        }

        @Override
        protected RowInRecordMapper getNegativeRowMapper(ColumnModel columnModel, Map<String, OverlayModel> overlayModels,
                long estimatedRowCount, ChangeContext context) throws OperationException {
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

    private class OpaqueRowMapOperation extends RowMapOperation {

        protected OpaqueRowMapOperation(EngineConfig engineConfig) {
            super(engineConfig);
        }

        @Override
        public String getDescription() {
            return "an operation which does not declare its dependencies or the columns it touches";
        }

    }

    @SuppressWarnings("unchecked")
    @BeforeMethod
    public void setUp() {
        changeContext = spy(new ChangeContext() {

            @Override
            public long getHistoryEntryId() {
                return historyEntryId;
            }

            @Override
            public long getProjectId() {
                return projectId;
            }

            @Override
            public String getChangeDescription() {
                return "change description";
            }

            @Override
            public <T> ChangeData<T> getChangeData(String dataId, ChangeDataSerializer<T> serializer,
                    BiFunction<Grid, Optional<ChangeData<T>>, ChangeData<T>> completionProcess,
                    List<ColumnId> dependencies, Mode engineMode) throws IOException {
                return completionProcess.apply(initialGrid, Optional.empty());
            }

        });

        initialGrid = mock(Grid.class);
        rowMappedGrid = mock(Grid.class);
        recordMappedGrid = mock(Grid.class);
        rowJoinedGridNeutralEngine = mock(Grid.class);
        rowJoinedGridWithFacet = mock(Grid.class);
        recordJoinedGrid = mock(Grid.class);
        opaquelyMappedGrid = mock(Grid.class);

        rowMappedChangeData = mock(ChangeDataRow.class);
        recordMappedChangeData = mock(ChangeDataListRow.class);

        initialColumnModel = new ColumnModel(Arrays.asList(
                new ColumnMetadata("A"),
                new ColumnMetadata("B").withReconConfig(reconConfigA),
                new ColumnMetadata("C"),
                new ColumnMetadata("D").withReconConfig(reconConfigA)));
        when(initialGrid.getColumnModel()).thenReturn(initialColumnModel);

        mappedColumnModelLazy = new ColumnModel(Arrays.asList(
                new ColumnMetadata("A", "A", 0L, null),
                new ColumnMetadata("E", "E", 1234L, null),
                new ColumnMetadata("overwritten B", "overwritten B", 1234L, reconConfigB),
                new ColumnMetadata("A", "D", 0L, reconConfigA),
                new ColumnMetadata("B", "new B", 0L, reconConfigA)));
        mappedColumnModelEagerNeutralEngine = new ColumnModel(Arrays.asList(
                new ColumnMetadata("B", "new B", 0L, reconConfigA),
                new ColumnMetadata("A"),
                new ColumnMetadata("B").withReconConfig(reconConfigA),
                new ColumnMetadata("D").withReconConfig(reconConfigA)));
        mappedColumnModelEagerWithFacet = new ColumnModel(Arrays.asList(
                new ColumnMetadata("B", "new B", 1234L, reconConfigA), // even though the column B is copied, it is only
                                                                       // for a subset of the dataset as we have a facet
                new ColumnMetadata("A"),
                new ColumnMetadata("B").withReconConfig(reconConfigA),
                new ColumnMetadata("D").withReconConfig(reconConfigA)));
        mappedColumnModelOpaque = new ColumnModel(Arrays.asList(
                new ColumnMetadata("A").withLastModified(historyEntryId),
                new ColumnMetadata("B").withReconConfig(reconConfigA).withLastModified(historyEntryId),
                new ColumnMetadata("C").withLastModified(historyEntryId),
                new ColumnMetadata("D").withReconConfig(reconConfigA).withLastModified(historyEntryId)));
        when(rowMappedGrid.getColumnModel()).thenReturn(mappedColumnModelLazy);
        when(recordMappedGrid.getColumnModel()).thenReturn(mappedColumnModelLazy);

        RowMapper anyRowMapper = any();
        when(initialGrid.mapRows(anyRowMapper, eq(mappedColumnModelLazy)))
                .thenReturn(rowMappedGrid);
        RecordMapper anyRecordMapper = any();
        when(initialGrid.mapRecords(anyRecordMapper, eq(mappedColumnModelLazy)))
                .thenReturn(recordMappedGrid);
        anyRowMapper = any();
        when(initialGrid.mapRows(anyRowMapper, eq(mappedColumnModelOpaque)))
                .thenReturn(opaquelyMappedGrid);
        when(initialGrid.mapRows(any(RowFilter.class), any(), any(Optional.class)))
                .thenReturn(rowMappedChangeData);
        when(initialGrid.mapRecords(any(RecordFilter.class), any(), any(Optional.class)))
                .thenReturn(recordMappedChangeData);
        when(initialGrid.join(eq(rowMappedChangeData), (RowChangeDataJoiner<Row>) any(), eq(mappedColumnModelEagerNeutralEngine)))
                .thenReturn(rowJoinedGridNeutralEngine);
        when(initialGrid.join(eq(rowMappedChangeData), (RowChangeDataJoiner<Row>) any(), eq(mappedColumnModelEagerWithFacet)))
                .thenReturn(rowJoinedGridWithFacet);
        when(initialGrid.join(eq(recordMappedChangeData), (RecordChangeDataJoiner<List<Row>>) any(),
                eq(mappedColumnModelEagerNeutralEngine)))
                .thenReturn(recordJoinedGrid);

        when(rowMappedGrid.withOverlayModels(any()))
                .thenReturn(rowMappedGrid);
        when(recordMappedGrid.withOverlayModels(any()))
                .thenReturn(recordMappedGrid);
        when(rowJoinedGridNeutralEngine.withOverlayModels(any()))
                .thenReturn(rowJoinedGridNeutralEngine);
        when(rowJoinedGridWithFacet.withOverlayModels(any()))
                .thenReturn(rowJoinedGridWithFacet);
        when(recordJoinedGrid.withOverlayModels(any()))
                .thenReturn(recordJoinedGrid);
        when(opaquelyMappedGrid.withOverlayModels(any()))
                .thenReturn(opaquelyMappedGrid);

        FacetConfig facetConfig = mock(FacetConfig.class);
        Facet facet = mock(Facet.class);
        when(facetConfig.isNeutral()).thenReturn(false);
        when(facetConfig.getColumnDependencies()).thenReturn(Collections.singleton("C"));
        when(facetConfig.apply(initialColumnModel, Collections.emptyMap(), projectId)).thenReturn(facet);
        engineConfigWithFacet = new EngineConfig(Collections.singletonList(facetConfig), Mode.RowBased);

        SUT_rowsEagerWithFacet = new EagerRowMapOperation(engineConfigWithFacet);

    }

    @Test
    public void testApplyRowsLazily() throws OperationException {
        ChangeResult result = SUT_rowsLazy.apply(initialGrid, changeContext);

        verify(initialGrid, times(1)).mapRows(any(RowMapper.class), eq(mappedColumnModelLazy));
        verify(initialGrid, times(0)).mapRecords(any(RecordMapper.class), any(ColumnModel.class));
        verify(initialGrid, times(0)).mapRows(any(RowFilter.class), any(), any());
        verify(initialGrid, times(0)).mapRecords(any(RecordFilter.class), any(), any());
        assertEquals(result.getGrid(), rowMappedGrid);
    }

    @Test
    public void testApplyRecordsLazily() throws OperationException {
        ChangeResult result = SUT_recordsLazy.apply(initialGrid, changeContext);

        verify(initialGrid, times(1)).mapRecords(any(RecordMapper.class), eq(mappedColumnModelLazy));
        verify(initialGrid, times(0)).mapRows(any(RowMapper.class), any(ColumnModel.class));
        verify(initialGrid, times(0)).mapRows(any(RowFilter.class), any(), any());
        verify(initialGrid, times(0)).mapRecords(any(RecordFilter.class), any(), any());
        assertEquals(result.getGrid(), recordMappedGrid);
    }

    @Test
    public void testApplyRowsEagerly() throws OperationException, IOException {
        ChangeResult result = SUT_rowsEager.apply(initialGrid, changeContext);

        verify(initialGrid, times(1)).mapRows(any(RowFilter.class), any(), eq(Optional.empty()));
        verify(initialGrid, times(0)).mapRows(any(RowMapper.class), any(ColumnModel.class));
        verify(initialGrid, times(0)).mapRecords(any(RecordMapper.class), any(ColumnModel.class));
        verify(initialGrid, times(0)).mapRecords(any(RecordFilter.class), any(), any());
        List<ColumnId> columnIds = Arrays.asList(new ColumnId("A", 0L), new ColumnId("D", 0L));
        verify(changeContext, times(1)).getChangeData(eq("eval"), any(), any(), eq(columnIds), eq(Mode.RowBased));
        assertEquals(result.getGrid(), rowJoinedGridNeutralEngine);
    }

    @Test
    public void testApplyRowsEagerlyWithFacet() throws OperationException, IOException {
        ChangeResult result = SUT_rowsEagerWithFacet.apply(initialGrid, changeContext);

        verify(initialGrid, times(1)).mapRows(any(RowFilter.class), any(), eq(Optional.empty()));
        verify(initialGrid, times(0)).mapRows(any(RowMapper.class), any(ColumnModel.class));
        verify(initialGrid, times(0)).mapRecords(any(RecordMapper.class), any(ColumnModel.class));
        verify(initialGrid, times(0)).mapRecords(any(RecordFilter.class), any(), any());
        List<ColumnId> columnIds = Arrays.asList(new ColumnId("A", 0L), new ColumnId("D", 0L), new ColumnId("C", 0L));
        verify(changeContext, times(1)).getChangeData(eq("eval"), any(), any(), eq(columnIds), eq(Mode.RowBased));
        assertEquals(result.getGrid(), rowJoinedGridWithFacet);
    }

    @Test
    public void testApplyRecordsEagerly() throws OperationException, IOException {
        ChangeResult result = SUT_recordsEager.apply(initialGrid, changeContext);

        verify(initialGrid, times(1)).mapRecords(any(RecordFilter.class), any(), eq(Optional.empty()));
        verify(initialGrid, times(0)).mapRecords(any(RecordMapper.class), any(ColumnModel.class));
        verify(initialGrid, times(0)).mapRows(any(RowMapper.class), any(ColumnModel.class));
        verify(initialGrid, times(0)).mapRows(any(RowFilter.class), any(), any());
        List<ColumnId> columnIds = Arrays.asList(new ColumnId("A", 0L), new ColumnId("D", 0L));
        verify(changeContext, times(1)).getChangeData(eq("eval"), any(), any(), eq(columnIds), eq(Mode.RecordBased));
        assertEquals(result.getGrid(), recordJoinedGrid);
    }

    @Test
    public void testApplyRowsOpaquely() throws OperationException {
        ChangeResult result = SUT_rowsOpaque.apply(initialGrid, changeContext);

        verify(initialGrid, times(1)).mapRows(any(RowMapper.class), eq(mappedColumnModelOpaque));
        verify(initialGrid, times(0)).mapRecords(any(RecordMapper.class), any(ColumnModel.class));
        verify(initialGrid, times(0)).mapRows(any(RowFilter.class), any(), any());
        verify(initialGrid, times(0)).mapRecords(any(RecordFilter.class), any(), any());
        assertEquals(result.getGrid(), opaquelyMappedGrid);
    }

    @Test
    public void testMissingDependency() {
        ColumnModel columnModel = new ColumnModel(Arrays.asList(
                new ColumnMetadata("A"),
                new ColumnMetadata("B"),
                new ColumnMetadata("D")));
        Grid grid = mock(Grid.class);
        when(grid.getColumnModel()).thenReturn(columnModel);

        assertThrows(MissingColumnException.class, () -> SUT_rowsEager.apply(grid, changeContext));
    }

    @Test
    public void testMissingDeletedColumn() {
        ColumnModel columnModel = new ColumnModel(Arrays.asList(
                new ColumnMetadata("A"),
                new ColumnMetadata("B"),
                new ColumnMetadata("D")));
        Grid grid = mock(Grid.class);
        when(grid.getColumnModel()).thenReturn(columnModel);

        assertThrows(MissingColumnException.class, () -> SUT_rowsLazy.apply(grid, changeContext));
    }

    @Test
    public void testMissingCopiedColumn() {
        ColumnModel columnModel = new ColumnModel(Arrays.asList(
                new ColumnMetadata("A"),
                new ColumnMetadata("C"),
                new ColumnMetadata("D")));
        Grid grid = mock(Grid.class);
        when(grid.getColumnModel()).thenReturn(columnModel);

        assertThrows(MissingColumnException.class, () -> SUT_rowsLazy.apply(grid, changeContext));
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

        assertThrows(DuplicateColumnException.class, () -> SUT_rowsLazy.apply(grid, changeContext));
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

        assertThrows(DuplicateColumnException.class, () -> SUT_rowsLazy.apply(grid, changeContext));
    }

    @Test
    public void testChangeDataJoinerRow() {
        Engine engine = mock(Engine.class);
        when(engine.getMode()).thenReturn(Mode.RowBased);
        RowFilter rowFilter = mock(RowFilter.class);
        when(engine.combinedRowFilters()).thenReturn(rowFilter);
        ColumnModel columnModel = mock(ColumnModel.class);
        when(columnModel.getKeyColumnIndex()).thenReturn(0);

        Row positiveRow = new Row(Arrays.asList(new Cell("a", null), new Cell("b", null), new Cell("c", null)), true, false);
        when(rowFilter.filterRow(123L, positiveRow)).thenReturn(true);
        Row negativeRow = new Row(Arrays.asList(new Cell("d", null), new Cell("e", null), new Cell("f", null)), false, true);
        when(rowFilter.filterRow(123L, negativeRow)).thenReturn(false);
        Row mappedRow = new Row(Arrays.asList(new Cell("x", null)));

        List<Integer> positiveIndexMap = Arrays.asList(0, 1, -1, 1);
        List<Integer> negativeIndexMap = Arrays.asList(0, 1, 2, 1);

        RowInRecordChangeDataJoiner SUT = RowMapOperation.joinerWithInsertions(positiveIndexMap, negativeIndexMap, columnModel, engine);

        Row positiveResult = SUT.call(null, positiveRow, new IndexedData<Row>(123L, mappedRow));
        Row negativeResult = SUT.call(null, negativeRow, new IndexedData<Row>(123L, mappedRow));

        Row expectedPositiveResult = new Row(
                Arrays.asList(new Cell("a", null), new Cell("b", null), new Cell("x", null), new Cell("b", null)), true, false);
        Row expectedNegativeResult = new Row(
                Arrays.asList(new Cell("d", null), new Cell("e", null), new Cell("f", null), new Cell("e", null)), false, true);

        assertEquals(positiveResult, expectedPositiveResult);
        assertEquals(negativeResult, expectedNegativeResult);
    }

    @Test
    public void testChangeDataJoinerRecord() {
        Engine engine = mock(Engine.class);
        when(engine.getMode()).thenReturn(Mode.RecordBased);
        RecordFilter recordFilter = mock(RecordFilter.class);
        when(engine.combinedRecordFilters()).thenReturn(recordFilter);
        ColumnModel columnModel = mock(ColumnModel.class);
        when(columnModel.getKeyColumnIndex()).thenReturn(0);

        Row positiveRow = new Row(Arrays.asList(new Cell("a", null), new Cell("b", null), new Cell("c", null)), true, false);
        Record positiveRecord = new Record(123L, Arrays.asList(positiveRow));
        when(recordFilter.filterRecord(positiveRecord)).thenReturn(true);
        Row negativeRow = new Row(Arrays.asList(new Cell("d", null), new Cell("e", null), new Cell("f", null)), false, true);
        Record negativeRecord = new Record(123L, Arrays.asList(negativeRow));
        when(recordFilter.filterRecord(negativeRecord)).thenReturn(false);
        Row mappedRow = new Row(Arrays.asList(new Cell("x", null)));

        List<Integer> positiveIndexMap = Arrays.asList(0, 1, -1, 1);
        List<Integer> negativeIndexMap = Arrays.asList(0, 1, 2, 1);

        RowInRecordChangeDataJoiner SUT = RowMapOperation.joinerWithInsertions(positiveIndexMap, negativeIndexMap, columnModel, engine);

        List<Row> positiveResult = SUT.call(positiveRecord, new IndexedData<List<Row>>(123L, Collections.singletonList(mappedRow)));
        List<Row> negativeResult = SUT.call(negativeRecord, new IndexedData<List<Row>>(123L, Collections.singletonList(mappedRow)));

        Row expectedPositiveResult = new Row(
                Arrays.asList(new Cell("a", null), new Cell("b", null), new Cell("x", null), new Cell("b", null)), true, false);
        Row expectedNegativeResult = new Row(
                Arrays.asList(new Cell("d", null), new Cell("e", null), new Cell("f", null), new Cell("e", null)), false, true);

        assertEquals(positiveResult, Arrays.asList(expectedPositiveResult));
        assertEquals(negativeResult, Arrays.asList(expectedNegativeResult));
    }

    @Test
    public void testDefaultJoiner() {
        RowInRecordMapper negativeMapper = mock(RowInRecordMapper.class);
        Record record = mock(Record.class);
        Row currentRow = mock(Row.class);
        Row computedRow = mock(Row.class);
        Row negativelyMappedRow = mock(Row.class);
        when(negativeMapper.call(record, 123L, currentRow)).thenReturn(negativelyMappedRow);

        IndexedData<Row> indexedData = new IndexedData<>(123L, computedRow);
        IndexedData<Row> absentIndexedData = new IndexedData<>(123L, null);

        RowInRecordChangeDataJoiner SUT = RowMapOperation.defaultJoiner(negativeMapper);

        Row mappedRowInsideView = SUT.call(record, currentRow, indexedData);
        assertEquals(mappedRowInsideView, computedRow);

        Row mappedRowOutsideView = SUT.call(record, currentRow, absentIndexedData);
        assertEquals(mappedRowOutsideView, negativelyMappedRow);
    }

    @Test
    public void testMapperWithInsertions() {
        List<Integer> indexMap = Arrays.asList(0, 2, -1, 1, -2);
        RowInRecordMapper origMapper = mock(RowInRecordMapper.class);
        Record record = mock(Record.class);
        Row row = new Row(Arrays.asList(new Cell("a", null), new Cell("b", null), new Cell("c", null)), true, false);
        when(origMapper.call(record, 123L, row))
                .thenReturn(new Row(Arrays.asList(new Cell("A", null), new Cell("B", null)), true, false));

        RowInRecordMapper SUT = RowMapOperation.mapperWithInsertions(origMapper, indexMap);

        Row mappedRow = SUT.call(record, 123L, row);

        assertEquals(mappedRow, new Row(Arrays.asList(
                new Cell("a", null), new Cell("c", null), new Cell("A", null), new Cell("b", null), new Cell("B", null)), true, false));
    }

    private static abstract class ChangeDataRow implements ChangeData<Row> {

    }

    private static abstract class ChangeDataListRow implements ChangeData<List<Row>> {

    }
}
