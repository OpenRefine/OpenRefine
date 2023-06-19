
package org.openrefine.runners.testing;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.NotImplementedException;
import org.openrefine.browsing.columns.ColumnStats;
import org.openrefine.browsing.facets.AllFacetsAggregator;
import org.openrefine.browsing.facets.AllFacetsState;
import org.openrefine.browsing.facets.Facet;
import org.openrefine.browsing.facets.FacetResult;
import org.openrefine.browsing.facets.FacetState;
import org.openrefine.browsing.facets.RecordAggregator;
import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.browsing.facets.StringFacet;
import org.openrefine.browsing.facets.StringFacetState;
import org.openrefine.importers.MultiFileReadingProgress;
import org.openrefine.importers.MultiFileReadingProgressStub;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Grid.ApproxCount;
import org.openrefine.model.Grid.PartialAggregation;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Record;
import org.openrefine.model.RecordFilter;
import org.openrefine.model.RecordMapper;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.model.RowFlatMapper;
import org.openrefine.model.RowMapper;
import org.openrefine.model.RowScanMapper;
import org.openrefine.model.Runner;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.model.changes.RecordChangeDataJoiner;
import org.openrefine.model.changes.RecordChangeDataProducer;
import org.openrefine.model.changes.RowChangeDataFlatJoiner;
import org.openrefine.model.changes.RowChangeDataJoiner;
import org.openrefine.model.changes.RowChangeDataProducer;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.process.ProgressReporterStub;
import org.openrefine.process.ProgressingFuture;
import org.openrefine.sorting.NumberCriterion;
import org.openrefine.sorting.SortingConfig;
import org.openrefine.sorting.StringCriterion;
import org.openrefine.util.CloseableIterable;
import org.openrefine.util.CloseableIterator;
import org.openrefine.util.TestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * A collection of generic tests that any implementation of {@link Runner} should satisfy. These tests are provided in
 * this module so that other implementations can reuse this test class.
 * 
 * @author Antonin Delpeuch
 *
 */
public abstract class RunnerTestBase {

    protected Runner SUT;

    protected Grid simpleGrid, longerGrid, gridToSort;
    protected ChangeData<String> simpleChangeData;
    protected List<Row> expectedRows;
    protected List<Record> expectedRecords;
    protected SortingConfig sortingConfig;
    protected OverlayModel overlayModel;
    protected Charset utf8 = Charset.forName("UTF-8");

    protected File tempDir;

    public abstract Runner getDatamodelRunner() throws IOException;

    @BeforeClass
    public void setUp() throws IOException {
        SUT = getDatamodelRunner();
        tempDir = TestUtils.createTempDirectory("datamodelrunnertest");
    }

    @AfterClass
    public void tearDown() {
        SUT = null;
        try {
            FileUtils.deleteDirectory(tempDir);
        } catch (IOException e) {
            ;
        }
        tempDir = null;
    }

    protected Grid createGrid(String[] columnNames, Cell[][] cells) {
        ColumnModel cm = new ColumnModel(Arrays.asList(columnNames)
                .stream()
                .map(name -> new ColumnMetadata(name))
                .collect(Collectors.toList()));
        List<Row> rows = new ArrayList<>(cells.length);
        for (int i = 0; i != cells.length; i++) {
            rows.add(new Row(Arrays.asList(cells[i])));
        }
        return SUT.gridFromList(cm, rows, Collections.emptyMap());
    }

    protected Grid createGrid(String[] columnNames, Serializable[][] cellValues) {
        Cell[][] cells = new Cell[cellValues.length][];
        for (int i = 0; i != cellValues.length; i++) {
            cells[i] = new Cell[cellValues[i].length];
            for (int j = 0; j != cellValues[i].length; j++) {
                if (cellValues[i][j] != null) {
                    cells[i][j] = new Cell(cellValues[i][j], null);
                } else {
                    cells[i][j] = null;
                }
            }
        }
        return createGrid(columnNames, cells);
    }

    protected <T extends Serializable> ChangeData<T> createChangeData(@SuppressWarnings("unchecked") IndexedData<T>... data) {
        return SUT.changeDataFromList(Arrays.asList(data));
    }

    @SuppressWarnings("unchecked")
    @BeforeMethod
    public void setUpExamples() {
        simpleGrid = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { "", 1 },
                        { "c", true },
                        { null, 123123123123L }
                });
        longerGrid = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { "", "d" },
                        { "", 1 },
                        { "c", true },
                        { "e", "f" },
                        { null, 123123123123L }
                });
        gridToSort = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "c", 1 },
                        { "a", 1 },
                        { null, 0 },
                        { "a", 5 }
                });
        simpleChangeData = createChangeData(
                new IndexedData<String>(0L, "first"),
                new IndexedData<String>(2L, "third"),
                new IndexedData<String>(3L, null));
        expectedRows = Arrays.asList(
                row("a", "b"),
                row("", 1),
                row("c", true),
                row(null, 123123123123L));
        expectedRecords = Arrays.asList(
                new Record(0L, Arrays.asList(
                        expectedRows.get(0),
                        expectedRows.get(1))),
                new Record(2L, Arrays.asList(
                        expectedRows.get(2),
                        expectedRows.get(3))));

        NumberCriterion numberCriterion = new NumberCriterion();
        numberCriterion.columnName = "bar";
        StringCriterion stringCriterion = new StringCriterion();
        stringCriterion.columnName = "foo";
        sortingConfig = new SortingConfig(
                Arrays.asList(numberCriterion, stringCriterion));
        overlayModel = new OverlayModel() {
        };

    }

    protected Row row(Serializable... values) {
        return new Row(Arrays.asList(values).stream().map(v -> v == null ? null : new Cell(v, null)).collect(Collectors.toList()));
    }

    @Test
    public void testAccessMetadata() {
        Assert.assertEquals(simpleGrid.getColumnModel(),
                new ColumnModel(Arrays.asList(new ColumnMetadata("foo"), new ColumnMetadata("bar"))));
        Assert.assertEquals(simpleGrid.getOverlayModels(), Collections.emptyMap());
    }

    @Test
    public void testWithOverlayModel() {
        Grid withOverlayModel = simpleGrid.withOverlayModels(Collections.singletonMap("foo", overlayModel));
        Map<String, OverlayModel> overlayModels = withOverlayModel.getOverlayModels();
        Assert.assertEquals(overlayModels.get("foo"), overlayModel);
        Assert.assertNull(overlayModels.get("bar"));
    }

    @Test
    public void testDatamodelRunner() {
        Assert.assertNotNull(simpleGrid.getRunner());
    }

    protected static RowFilter myRowFilter = new RowFilter() {

        private static final long serialVersionUID = -8386034714884614567L;

        @Override
        public boolean filterRow(long rowIndex, Row row) {
            return rowIndex != 1L;
        }
    };

    @Test
    public void testAccessRows() {
        Grid state = simpleGrid;

        Assert.assertEquals(state.rowCount(), 4L);

        Assert.assertEquals(state.getRow(0L), expectedRows.get(0));
        Assert.assertEquals(state.getRow(3L), expectedRows.get(3));
        try {
            state.getRow(4L);
            Assert.fail("No exception thrown by an out of bounds access");
        } catch (IndexOutOfBoundsException e) {
            ;
        }

    }

    @Test
    public void testGetRowsAfter() {
        Assert.assertEquals(
                simpleGrid.getRowsAfter(1L, 2).stream().map(ir -> ir.getRow()).collect(Collectors.toList()),
                expectedRows.subList(1, 3));

        Assert.assertEquals(simpleGrid.getRowsAfter(myRowFilter, 0L, 2),
                Arrays.asList(new IndexedRow(0L, expectedRows.get(0)),
                        new IndexedRow(2L, expectedRows.get(2))));
        Assert.assertEquals(simpleGrid.getRowsAfter(myRowFilter, 2L, 2),
                Arrays.asList(new IndexedRow(2L, expectedRows.get(2)),
                        new IndexedRow(3L, expectedRows.get(3))));
    }

    /*
     * { "a", "b" }, { "", 1 }, { "c", true }, { null, 123123123123L }
     */
    @Test
    public void testGetRowsBefore() {
        Assert.assertEquals(
                simpleGrid.getRowsBefore(3L, 2).stream().map(ir -> ir.getRow()).collect(Collectors.toList()),
                expectedRows.subList(1, 3));
        Assert.assertEquals(simpleGrid.getRowsBefore(5L, 1),
                Collections.singletonList(new IndexedRow(3L, expectedRows.get(3))));

        Assert.assertEquals(simpleGrid.getRowsBefore(myRowFilter, 2L, 2),
                Arrays.asList(new IndexedRow(0L, expectedRows.get(0))));
        Assert.assertEquals(simpleGrid.getRowsBefore(myRowFilter, 3L, 2),
                Arrays.asList(new IndexedRow(0L, expectedRows.get(0)),
                        new IndexedRow(2L, expectedRows.get(2))));
    }

    @Test
    public void testGetRowsById() {
        List<Long> indices = Arrays.asList(-1L, 0L, 2L, 5L);
        List<IndexedRow> expected = Arrays.asList(
                null,
                new IndexedRow(0L, simpleGrid.getRow(0L)),
                new IndexedRow(2L, simpleGrid.getRow(2L)),
                null);

        List<IndexedRow> rows = simpleGrid.getRows(indices);
        Assert.assertEquals(rows, expected);
    }

    @Test
    public void testAccessRowsOutOfBounds() {
        Assert.assertEquals(simpleGrid.getRowsAfter(5L, 3), Collections.emptyList());

        Assert.assertEquals(
                gridToSort.getRowsAfter(RowFilter.ANY_ROW, 30, 10),
                Collections.emptyList());
    }

    protected static RecordFilter myRecordFilter = new RecordFilter() {

        private static final long serialVersionUID = 4197928472022711691L;

        @Override
        public boolean filterRecord(Record record) {
            return record.getLogicalStartRowId() == 2L;
        }
    };

    @Test
    public void testIterateRowsFilter() {
        try (CloseableIterator<IndexedRow> indexedRows = simpleGrid.iterateRows(myRowFilter)) {
            Assert.assertTrue(indexedRows.hasNext());
            Assert.assertEquals(indexedRows.next(), new IndexedRow(0L, expectedRows.get(0)));
            Assert.assertTrue(indexedRows.hasNext());
            Assert.assertEquals(indexedRows.next(), new IndexedRow(2L, expectedRows.get(2)));
            Assert.assertTrue(indexedRows.hasNext());
        }
    }

    @Test
    public void testCountMatchingRows() {
        Assert.assertEquals(simpleGrid.countMatchingRows(myRowFilter), 3);
    }

    @Test
    public void testCountMatchingRecordsApproxOvershoot() {
        // with a limit that overshoots the total number of rows
        ApproxCount count = simpleGrid.countMatchingRecordsApprox(myRecordFilter, 10);
        Assert.assertEquals(count.getMatched(), 1);
        Assert.assertEquals(count.getProcessed(), 2);
    }

    @Test
    public void testCountMatchingRecordsApproxZero() {
        // with 0 as a limit
        ApproxCount count = simpleGrid.countMatchingRecordsApprox(myRecordFilter, 0);
        Assert.assertEquals(count.getMatched(), 0);
        Assert.assertEquals(count.getProcessed(), 0);
    }

    @Test
    public void testCountMatchingRecordsApproxIntermediate() {
        // the way the datamodel implementation selects rows is unspecified.
        // it can also filter slightly less or more than the limit
        ApproxCount count = simpleGrid.countMatchingRecordsApprox(myRecordFilter, 2);
        Assert.assertTrue(count.getMatched() <= count.getProcessed());
        Assert.assertTrue(count.getProcessed() <= 2);
    }

    @Test
    public void testAccessRecords() {
        Grid state = simpleGrid;

        Assert.assertEquals(state.recordCount(), 2L);

        Assert.assertEquals(state.getRecord(0L), expectedRecords.get(0));
        Assert.assertEquals(state.getRecord(2L), expectedRecords.get(1));
        try {
            state.getRecord(1L);
            Assert.fail("No exception thrown by an out of bounds access");
        } catch (IllegalArgumentException e) {
            ;
        }
    }

    @Test
    public void testGetRecordsAfter() {
        Assert.assertEquals(simpleGrid.getRecordsAfter(1L, 2), expectedRecords.subList(1, 2));

        Assert.assertEquals(simpleGrid.getRecordsAfter(myRecordFilter, 0L, 3),
                Collections.singletonList(expectedRecords.get(1)));
    }

    @Test
    public void testGetRecordsBefore() {
        Assert.assertEquals(simpleGrid.getRecordsBefore(3L, 2), expectedRecords.subList(0, 2));
        Assert.assertEquals(simpleGrid.getRecordsBefore(0L, 2), Collections.emptyList());

        Assert.assertEquals(simpleGrid.getRecordsBefore(myRecordFilter, 3L, 2),
                Collections.singletonList(expectedRecords.get(1)));
    }

    @Test
    public void testRecordGroupingNoRecordStart() {
        Grid noRecordStart = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { null, "a" },
                        { "", "b" },
                        { null, "c" },
                        { null, "d" },
                        { null, "e" },
                        { null, "f" }
                });

        List<Record> records = noRecordStart.collectRecords();
        Assert.assertEquals(records.size(), 1);
        Assert.assertEquals(records.get(0).getRows(),
                noRecordStart.collectRows().stream().map(IndexedRow::getRow).collect(Collectors.toList()));
    }

    @Test
    public void testRecordsRespectKeyColumnIndex() {
        Grid state = simpleGrid.withColumnModel(simpleGrid.getColumnModel().withKeyColumnIndex(1));

        Assert.assertEquals(state.recordCount(), 4L);
        List<Record> records = Arrays.asList(
                new Record(0L, Arrays.asList(
                        expectedRows.get(0))),
                new Record(1L, Arrays.asList(
                        expectedRows.get(1))),
                new Record(2L, Arrays.asList(
                        expectedRows.get(2))),
                new Record(3L, Arrays.asList(
                        expectedRows.get(3))));
        Assert.assertEquals(state.collectRecords(), records);
    }

    @Test
    public void testIterateRecordsFilter() {
        try (CloseableIterator<Record> records = simpleGrid.iterateRecords(myRecordFilter).iterator()) {
            Assert.assertTrue(records.hasNext());
            Assert.assertEquals(records.next(), expectedRecords.get(1));
            Assert.assertFalse(records.hasNext());
        }
    }

    @Test
    public void testCountMatchingRecords() {
        Assert.assertEquals(simpleGrid.countMatchingRecords(myRecordFilter), 1);
    }

    @Test
    public void testCountMatchingRowsApproxOvershoot() {
        // with a limit that overshoots the total number of rows
        ApproxCount count = simpleGrid.countMatchingRowsApprox(myRowFilter, 10);
        Assert.assertEquals(count.getMatched(), 3);
        Assert.assertEquals(count.getProcessed(), 4);
    }

    @Test
    public void testCountMatchingRowsApproxZero() {
        // with 0 as a limit
        ApproxCount count = simpleGrid.countMatchingRowsApprox(myRowFilter, 0);
        Assert.assertEquals(count.getMatched(), 0);
        Assert.assertEquals(count.getProcessed(), 0);
    }

    @Test
    public void testCountMatchingRowsApproxIntermediate() {
        // the way the datamodel implementation selects rows is unspecified.
        // it can also filter slightly less than the limit
        ApproxCount count = simpleGrid.countMatchingRowsApprox(myRowFilter, 2);
        Assert.assertTrue(count.getMatched() <= count.getProcessed());
        Assert.assertTrue(count.getProcessed() > 0);
    }

    @Test
    public void testRoundTripSerializationNoProgress() throws IOException, InterruptedException {
        File tempFile = new File(tempDir, "testgrid_no_progress");

        simpleGrid.saveToFile(tempFile);

        Grid loaded = SUT.loadGrid(tempFile);

        Assert.assertEquals(loaded.rowCount(), 4L);
        List<Row> actualRows = loaded.collectRows().stream().map(r -> r.getRow()).collect(Collectors.toList());
        Assert.assertEquals(actualRows, expectedRows);
        Assert.assertEquals(loaded.recordCount(), 2L);
        Assert.assertEquals(loaded.collectRecords(), expectedRecords);
        Assert.assertTrue(new File(tempFile, "grid/" + Runner.COMPLETION_MARKER_FILE_NAME).exists());
    }

    @Test
    public void testRoundTripSerializationAsync() throws IOException, InterruptedException, ExecutionException {
        File tempFile = new File(tempDir, "testgrid_progress");
        ProgressReporterStub reporter = new ProgressReporterStub();

        ProgressingFuture<Void> future = simpleGrid.saveToFileAsync(tempFile);
        future.onProgress(reporter);
        future.get();
        if (SUT.supportsProgressReporting()) {
            Assert.assertEquals(reporter.getPercentage(), 100);
        }

        Grid loaded = SUT.loadGrid(tempFile);

        Assert.assertEquals(loaded.rowCount(), 4L);
        List<Row> actualRows = loaded.collectRows().stream().map(r -> r.getRow()).collect(Collectors.toList());
        Assert.assertEquals(actualRows, expectedRows);
        Assert.assertEquals(loaded.recordCount(), 2L);
        Assert.assertEquals(loaded.collectRecords(), expectedRecords);
        Assert.assertTrue(new File(tempFile, "grid/" + Runner.COMPLETION_MARKER_FILE_NAME).exists());
    }

    @Test
    public void testReconSerialization() throws IOException {
        ColumnModel columnModel = new ColumnModel(Collections.singletonList(new ColumnMetadata("foo")));
        ReconCandidate candidate = new ReconCandidate("Q2334", "Hello World", new String[] {}, 89.3);
        Recon recon = new Recon(
                1234L, 5678L, Judgment.Matched, candidate, new Object[] {},
                Collections.singletonList(candidate), "http://my.service/api",
                "http://my.service/space", "http://my.service/schema", "batch", 0);
        Cell cell = new Cell("value", recon);
        List<Row> rows = Arrays.asList(new Row(Arrays.asList(cell)));
        Grid grid = SUT.gridFromList(columnModel, rows, Collections.emptyMap());

        File tempFile = new File(tempDir, "testgrid_recon");
        grid.saveToFile(tempFile);

        Grid loaded = SUT.loadGrid(tempFile);
        Assert.assertEquals(loaded.collectRows(), grid.collectRows());
    }

    @Test
    public void testLoadIncompleteGrid() throws IOException {
        File tempFile = new File(tempDir, "testgrid_incomplete");

        simpleGrid.saveToFile(tempFile);
        // artificially mark the grid as incomplete by removing the completion marker
        File completionMarker = new File(new File(tempFile, Grid.GRID_PATH), Runner.COMPLETION_MARKER_FILE_NAME);
        completionMarker.delete();

        try {
            SUT.loadGrid(tempFile);
            fail("loading an incomplete grid did not throw an exception");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void testComputeRowFacets() {
        Facet facetFoo = new StringFacet.Config("foo", "a").apply(simpleGrid.getColumnModel(), simpleGrid.getOverlayModels(), 1234L);
        Facet facetBar = new StringFacet.Config("bar", null).apply(simpleGrid.getColumnModel(), simpleGrid.getOverlayModels(), 1234L);

        List<Facet> facets = Arrays.asList(facetFoo, facetBar);
        List<FacetState> initialStates = facets
                .stream().map(facet -> facet.getInitialFacetState())
                .collect(Collectors.toList());

        AllFacetsAggregator aggregator = new AllFacetsAggregator(facets
                .stream().map(facet -> facet.getAggregator())
                .collect(Collectors.toList()));

        AllFacetsState states = simpleGrid.aggregateRows(aggregator, new AllFacetsState(ImmutableList.copyOf(initialStates),
                ImmutableList.of(ColumnStats.ZERO, ColumnStats.ZERO), 0L, 0L));

        List<FacetResult> facetResults = new ArrayList<>();
        for (int i = 0; i != states.size(); i++) {
            facetResults.add(facets.get(i).getFacetResult(states.get(i)));
        }

        List<FacetResult> results = facetResults;

        Assert.assertEquals(results.size(), 2);
        Assert.assertTrue(results.get(0) instanceof StringFacetState);
        Assert.assertTrue(results.get(1) instanceof StringFacetState);
        StringFacetState result1 = (StringFacetState) results.get(0);
        StringFacetState result2 = (StringFacetState) results.get(1);

        Map<String, Long> expectedMap = new HashMap<>();
        expectedMap.put("a", 1L);
        expectedMap.put("", 1L);
        expectedMap.put("c", 1L);
        expectedMap.put("null", 1L);
        Assert.assertEquals(result1.occurences, expectedMap);
        Assert.assertEquals(result2.occurences, Collections.singletonMap("b", 1L));
    }

    @Test
    public void testComputeRecordFacets() {
        Facet facetFoo = new StringFacet.Config("foo", "a").apply(simpleGrid.getColumnModel(), simpleGrid.getOverlayModels(), 1234L);
        Facet facetBar = new StringFacet.Config("bar", null).apply(simpleGrid.getColumnModel(), simpleGrid.getOverlayModels(), 1234L);

        List<Facet> facets = Arrays.asList(facetFoo, facetBar);
        List<FacetState> initialStates = facets
                .stream().map(facet -> facet.getInitialFacetState())
                .collect(Collectors.toList());

        AllFacetsAggregator aggregator = new AllFacetsAggregator(facets
                .stream().map(facet -> facet.getAggregator())
                .collect(Collectors.toList()));

        AllFacetsState states = simpleGrid.aggregateRecords(aggregator, new AllFacetsState(ImmutableList.copyOf(initialStates),
                ImmutableList.of(ColumnStats.ZERO, ColumnStats.ZERO), 0L, 0L));

        List<FacetResult> facetResults = new ArrayList<>();
        for (int i = 0; i != states.size(); i++) {
            facetResults.add(facets.get(i).getFacetResult(states.get(i)));
        }

        List<FacetResult> results = facetResults;

        Assert.assertEquals(results.size(), 2);
        Assert.assertTrue(results.get(0) instanceof StringFacetState);
        Assert.assertTrue(results.get(1) instanceof StringFacetState);
        StringFacetState result1 = (StringFacetState) results.get(0);
        StringFacetState result2 = (StringFacetState) results.get(1);

        Map<String, Long> expectedMap = new HashMap<>();
        expectedMap.put("a", 1L);
        expectedMap.put("", 1L);
        expectedMap.put("c", 1L);
        expectedMap.put("null", 1L);
        Assert.assertEquals(result1.occurences, expectedMap);
        expectedMap.clear();
        expectedMap.put("b", 1L);
        expectedMap.put("1", 1L);
        Assert.assertEquals(result2.occurences, expectedMap);
    }

    private static class BoxedLong implements FacetState {

        private static final long serialVersionUID = 7896255599935102833L;
        public final long value;

        public BoxedLong(long v) {
            value = v;
        }

        public static BoxedLong zero = new BoxedLong(0L);
    }

    private static class RowCounter implements RowAggregator<BoxedLong>, RecordAggregator<BoxedLong> {

        @Override
        public BoxedLong sum(BoxedLong first, BoxedLong second) {
            return new BoxedLong(first.value + second.value);
        }

        @Override
        public BoxedLong withRow(BoxedLong state, long rowId, Row row) {
            return new BoxedLong(state.value + 1);
        }

        @Override
        public BoxedLong withRecord(BoxedLong state, Record record) {
            return new BoxedLong(state.value + 1);
        }

    }

    @Test
    public void testAggregateRowsApproxPartialResult() {
        RowCounter aggregator = new RowCounter();
        PartialAggregation<BoxedLong> partialResult = simpleGrid.aggregateRowsApprox(aggregator, BoxedLong.zero, 2L);
        Assert.assertTrue(partialResult.getState().value > 0);
        Assert.assertTrue(partialResult.getProcessed() > 0);
    }

    @Test
    public void testAggregateRowsApproxFullResult() {
        RowCounter aggregator = new RowCounter();
        PartialAggregation<BoxedLong> fullResult = simpleGrid.aggregateRowsApprox(aggregator, BoxedLong.zero, 8L);
        Assert.assertEquals(fullResult.getState().value, 4L);
        Assert.assertFalse(fullResult.limitReached());
        Assert.assertEquals(fullResult.getProcessed(), 4L);
    }

    @Test
    public void testAggregateRecordsApproxPartialResult() {
        RowCounter aggregator = new RowCounter();
        PartialAggregation<BoxedLong> partialResult = gridToSort.aggregateRecordsApprox(aggregator, BoxedLong.zero, 2L);
        Assert.assertTrue(partialResult.getState().value > 0);
        Assert.assertTrue(partialResult.getProcessed() > 0);
    }

    @Test
    public void testAggregateRecordsApproxFullResult() {
        RowCounter aggregator = new RowCounter();
        PartialAggregation<BoxedLong> fullResult = gridToSort.aggregateRecordsApprox(aggregator, BoxedLong.zero, 8L);
        Assert.assertEquals(fullResult.getState().value, 3L);
        Assert.assertEquals(fullResult.getProcessed(), 3L);
        Assert.assertFalse(fullResult.limitReached());
    }

    public static RowMapper concatRowMapper = new RowMapper() {

        private static final long serialVersionUID = -2137895769820170019L;

        @Override
        public Row call(long rowId, Row row) {
            return row.withCell(1, new Cell(row.getCellValue(1).toString() + "_concat", null));
        }

        @Override
        public boolean preservesRecordStructure() {
            return true;
        }

    };

    @Test
    public void testMapRows() {
        Grid mapped = simpleGrid.mapRows(
                concatRowMapper, simpleGrid.getColumnModel());

        List<IndexedRow> rows = mapped.collectRows();
        Assert.assertEquals(rows.get(0).getRow().getCellValue(1), "b_concat");
        Assert.assertEquals(rows.get(1).getRow().getCellValue(1), "1_concat");
    }

    public static RowFlatMapper rowDuplicator = new RowFlatMapper() {

        private static final long serialVersionUID = -6205166282452082366L;

        @Override
        public List<Row> call(long rowId, Row row) {
            return Arrays.asList(row, row);
        }

    };

    @Test
    public void testFlatMapRows() {
        Grid mapped = simpleGrid.flatMapRows(
                rowDuplicator, simpleGrid.getColumnModel());

        Assert.assertEquals(mapped.getColumnModel(), simpleGrid.getColumnModel());
        List<Row> rows = mapped.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows.size(), 8);
        Assert.assertEquals(rows.get(0), rows.get(1));
        Assert.assertEquals(rows.get(0), simpleGrid.getRow(0L));
    }

    public static RowScanMapper<String> statefulRowMapper = new RowScanMapper<String>() {

        private static final long serialVersionUID = -2411339705543951236L;

        @Override
        public String feed(long rowId, Row row) {
            return row.getCellValue(1).toString();
        }

        @Override
        public String combine(String left, String right) {
            return left + right;
        }

        @Override
        public String unit() {
            return "";
        }

        @Override
        public Row map(String state, long rowId, Row row) {
            return row.withCell(1, new Cell(state + row.getCellValue(1).toString(), null));
        }

    };

    @Test
    public void testStatefullyMapRows() {
        Grid mapped = longerGrid.mapRows(
                statefulRowMapper, simpleGrid.getColumnModel());

        List<IndexedRow> rows = mapped.collectRows();
        Assert.assertEquals(rows.get(0).getRow().getCellValue(1), "b");
        Assert.assertEquals(rows.get(1).getRow().getCellValue(1), "bd");
        Assert.assertEquals(rows.get(2).getRow().getCellValue(1), "bd1");
        Assert.assertEquals(rows.get(3).getRow().getCellValue(1), "bd1true");
        Assert.assertEquals(rows.get(4).getRow().getCellValue(1), "bd1truef");
        Assert.assertEquals(rows.get(5).getRow().getCellValue(1), "bd1truef123123123123");
    }

    public static RecordMapper concatRecordMapper = RecordMapper.rowWiseRecordMapper(concatRowMapper);

    @Test
    public void testMapRecords() {
        Grid mapped = simpleGrid.mapRecords(
                concatRecordMapper, simpleGrid.getColumnModel());

        List<IndexedRow> rows = mapped.collectRows();
        Assert.assertEquals(rows.get(0).getRow().getCellValue(1), "b_concat");
        Assert.assertEquals(rows.get(1).getRow().getCellValue(1), "1_concat");
        Assert.assertEquals(rows.get(2).getRow().getCellValue(1), "true_concat");
    }

    @Test
    public void testReorderRowsPermanently() {
        Grid reordered = gridToSort.reorderRows(sortingConfig, true);

        Grid expected = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { null, 0 },
                        { "a", 1 },
                        { "c", 1 },
                        { "a", 5 }
                });

        assertGridEquals(reordered, expected);
    }

    protected static RowFilter rowFilterGreaterThanTwo = new RowFilter() {

        @Override
        public boolean filterRow(long rowIndex, Row row) {
            return rowIndex >= 2L;
        }
    };

    @Test
    public void testReorderRowsTemporarily() {
        Grid reordered = gridToSort.reorderRows(sortingConfig, false);

        List<IndexedRow> expectedRows = Arrays.asList(
                new IndexedRow(0L, 2L, new Row(Arrays.asList(null, new Cell(0, null)))),
                new IndexedRow(1L, 1L, new Row(Arrays.asList(new Cell("a", null), new Cell(1, null)))),
                new IndexedRow(2L, 0L, new Row(Arrays.asList(new Cell("c", null), new Cell(1, null)))),
                new IndexedRow(3L, 3L, new Row(Arrays.asList(new Cell("a", null), new Cell(5, null)))));

        Assert.assertEquals(reordered.getRow(0L), expectedRows.get(0).getRow());
        Assert.assertEquals(reordered.collectRows(), expectedRows);
        try (CloseableIterator<IndexedRow> reorderedIterator = reordered.iterateRows(RowFilter.ANY_ROW)) {
            Assert.assertEquals(IteratorUtils.toList(reorderedIterator), expectedRows);
        }

        Assert.assertEquals(reordered.getRowsAfter(0L, 2), expectedRows.subList(0, 2));
        // this assertion checks that the row id used for filtering is the original one, not the new one
        Assert.assertEquals(reordered.getRowsAfter(rowFilterGreaterThanTwo, 2L, 2), Collections.singletonList(expectedRows.get(3)));

        Assert.assertEquals(reordered.getRowsBefore(3L, 2), expectedRows.subList(1, 3));
        // this assertion checks that the row id used for filtering is the original one, not the new one
        Assert.assertEquals(reordered.getRowsBefore(rowFilterGreaterThanTwo, 2L, 1), Collections.singletonList(expectedRows.get(0)));
    }

    @Test
    public void testReorderRecordsPermanently() {
        Grid reordered = gridToSort.reorderRecords(sortingConfig, true);

        Grid expected = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", 1 },
                        { null, 0 },
                        { "c", 1 },
                        { "a", 5 }
                });

        assertGridEquals(reordered, expected);
    }

    @Test
    public void testReorderRecordsTemporarily() {
        Grid reordered = gridToSort.reorderRecords(sortingConfig, false);

        List<IndexedRow> expectedRows = Arrays.asList(
                new IndexedRow(0L, 1L, new Row(Arrays.asList(new Cell("a", null), new Cell(1, null)))),
                new IndexedRow(1L, 2L, new Row(Arrays.asList(null, new Cell(0, null)))),
                new IndexedRow(2L, 0L, new Row(Arrays.asList(new Cell("c", null), new Cell(1, null)))),
                new IndexedRow(3L, 3L, new Row(Arrays.asList(new Cell("a", null), new Cell(5, null)))));

        List<Record> expectedRecords = Arrays.asList(
                new Record(0L, 1L, Arrays.asList(expectedRows.get(0).getRow(), expectedRows.get(1).getRow())),
                new Record(2L, 0L, Collections.singletonList(expectedRows.get(2).getRow())),
                new Record(3L, 3L, Collections.singletonList(expectedRows.get(3).getRow())));

        Assert.assertEquals(reordered.getRecord(2L), expectedRecords.get(1));
        Assert.assertEquals(reordered.collectRows(), expectedRows);
        Assert.assertEquals(reordered.collectRecords(), expectedRecords);
        try (CloseableIterator<Record> recordIterator = reordered.iterateRecords(RecordFilter.ANY_RECORD).iterator()) {
            Assert.assertEquals(IteratorUtils.toList(recordIterator), expectedRecords);
        }

        Assert.assertEquals(reordered.getRecordsAfter(2L, 2), expectedRecords.subList(1, 3));
        Assert.assertEquals(reordered.getRecordsBefore(2L, 2), expectedRecords.subList(0, 1));
    }

    @Test
    public void testRemoveRows() {
        Grid removed = simpleGrid.removeRows(myRowFilter);

        Grid expected = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "", 1 }
                });

        assertGridEquals(removed, expected);
    }

    @Test
    public void testRemoveRecords() {
        Grid removed = simpleGrid.removeRecords(myRecordFilter);

        Grid expected = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { "", 1 }
                });

        Assert.assertEquals(removed.getColumnModel(), expected.getColumnModel());
        Assert.assertEquals(removed.collectRows(), expected.collectRows());
    }

    @Test
    public void testLimitRows() {
        Grid limited = simpleGrid.limitRows(2L);
        Grid expected = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { "", 1 }
                });

        Assert.assertEquals(limited.getColumnModel(), expected.getColumnModel());
        Assert.assertEquals(limited.collectRows(), expected.collectRows());
    }

    @Test
    public void testDropRows() {
        Grid limited = simpleGrid.dropRows(2L);
        Grid expected = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "c", true },
                        { null, 123123123123L }
                });

        Assert.assertEquals(limited.getColumnModel(), expected.getColumnModel());
        Assert.assertEquals(limited.collectRows(), expected.collectRows());
    }

    /**
     * Change data
     */

    private static ChangeDataSerializer<String> stringSerializer = new ChangeDataSerializer<String>() {

        private static final long serialVersionUID = -8853248982412622071L;

        @Override
        public String serialize(String changeDataItem) {
            return changeDataItem.strip();
        }

        @Override
        public String deserialize(String serialized) throws IOException {
            return serialized.strip();
        }

    };

    @Test(expectedExceptions = IOException.class)
    public void testLoadChangeDataDoesNotExist() throws IOException {
        SUT.loadChangeData(new File(tempDir, "doesNotExist"), stringSerializer);
    }

    @Test
    public void testSerializeChangeDataNoProgress() throws IOException, InterruptedException {
        File tempFile = new File(tempDir, "test_change_data");

        simpleChangeData.saveToFile(new File(tempFile, "data"), stringSerializer);

        ChangeData<String> loaded = SUT.loadChangeData(new File(tempFile, "data"), stringSerializer);

        Assert.assertNotNull(loaded.getRunner());
        Assert.assertEquals(loaded.get(0L), new IndexedData<>(0L, "first"));
        Assert.assertEquals(loaded.get(1L), new IndexedData<>(1L, null)); // not included in changedata
        Assert.assertEquals(loaded.get(2L), new IndexedData<>(2L, "third"));
        Assert.assertEquals(loaded.get(3L), new IndexedData<>(3L, null)); // null from creation

        // the change data was loaded from a file with a completion marker, so it is complete
        Assert.assertTrue(new File(tempFile, "data/" + Runner.COMPLETION_MARKER_FILE_NAME).exists());
        Assert.assertTrue(loaded.isComplete());
    }

    @Test
    public void testSerializeChangeDataAsync() throws IOException, InterruptedException, ExecutionException {
        File tempFile = new File(tempDir, "test_change_data_with_progress");
        ProgressReporterStub progress = new ProgressReporterStub();

        ProgressingFuture<Void> future = simpleChangeData.saveToFileAsync(new File(tempFile, "data"), stringSerializer);
        future.onProgress(progress);
        future.get();
        Assert.assertEquals(progress.getPercentage(), 100);

        ChangeData<String> loaded = SUT.loadChangeData(new File(tempFile, "data"), stringSerializer);

        Assert.assertNotNull(loaded.getRunner());
        Assert.assertEquals(loaded.get(0L), new IndexedData<>(0L, "first"));
        Assert.assertEquals(loaded.get(1L), new IndexedData<>(1L, null)); // not included in changedata
        Assert.assertEquals(loaded.get(2L), new IndexedData<>(2L, "third"));
        Assert.assertEquals(loaded.get(3L), new IndexedData<>(3L, null)); // null from creation

        // the change data was loaded from a file with a completion marker, so it is complete
        Assert.assertTrue(new File(tempFile, "data/" + Runner.COMPLETION_MARKER_FILE_NAME).exists());
        Assert.assertTrue(loaded.isComplete());
    }

    @Test
    public void testSerializeChangeDataDirAlreadyExists() throws IOException, InterruptedException {
        File tempFile = TestUtils.createTempDirectory("test_change_data_already_exists");

        simpleChangeData.saveToFile(tempFile, stringSerializer);

        ChangeData<String> loaded = SUT.loadChangeData(tempFile, stringSerializer);

        Assert.assertNotNull(loaded.getRunner());
        Assert.assertEquals(loaded.get(0L), new IndexedData<>(0L, "first"));
        Assert.assertEquals(loaded.get(1L), new IndexedData<>(1L, null));
        Assert.assertEquals(loaded.get(2L), new IndexedData<>(2L, "third"));
        Assert.assertEquals(loaded.get(3L), new IndexedData<>(3L, null));
    }

    @Test
    public void testIterateChangeData() {
        Iterator<IndexedData<String>> iterator = simpleChangeData.iterator();
        List<IndexedData<String>> actual = IteratorUtils.toList(iterator);
        List<IndexedData<String>> expected = new ArrayList<>();
        expected.add(new IndexedData<String>(0L, "first"));
        expected.add(new IndexedData<String>(2L, "third"));
        // nulls are skipped
        Assert.assertEquals(actual, expected);
    }

    public static RowChangeDataProducer<String> concatChangeMapper = new RowChangeDataProducer<String>() {

        private static final long serialVersionUID = -2137895769820170019L;

        @Override
        public String call(long rowId, Row row) {
            return row.getCellValue(1).toString() + "_concat";
        }

    };

    @Test
    public void testGenerateRowChangeData() {
        ChangeData<String> changeData = simpleGrid.mapRows(myRowFilter, concatChangeMapper, Optional.empty());

        Assert.assertEquals(changeData.get(0L), new IndexedData<>(0L, "b_concat"));
        Assert.assertEquals(changeData.get(1L), new IndexedData<>(1L, null)); // because it is excluded by the facet
    }

    @Test
    public void testLoadEmptyChangeData() throws IOException {
        File tempDir = TestUtils.createTempDirectory("empty_change_data");

        ChangeData<String> empty = SUT.loadChangeData(tempDir, stringSerializer);
        Assert.assertFalse(empty.isComplete());

        Assert.assertEquals(empty.get(0L), new IndexedData<>(0L));
        Iterator<IndexedData<String>> iterator = empty.iterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(iterator.next(), new IndexedData<>(0L));
    }

    @Test
    public void testComputeChangeDataFromIncompleteState() throws IOException, InterruptedException {
        ChangeData<String> incomplete = simpleGrid.mapRows(firstTwoRows, concatChangeMapper, Optional.empty());
        File tempFile = TestUtils.createTempDirectory("test_row_based_change_data_from_incomplete");
        incomplete.saveToFile(tempFile, stringSerializer);

        // artificially pretend that the change data is incomplete by removing the completion marker
        File completionMarker = new File(tempFile, Runner.COMPLETION_MARKER_FILE_NAME);
        completionMarker.delete();

        // reload the now properly incomplete change data
        incomplete = SUT.loadChangeData(tempFile, stringSerializer);
        Assert.assertFalse(incomplete.isComplete());

        // check that it returns incomplete IndexedData objects for unseen keys
        Assert.assertEquals(incomplete.get(2L), new IndexedData<>(2L));
        Assert.assertEquals(incomplete.get(3L), new IndexedData<>(3L));

        // complete it with more rows
        ChangeData<String> complete = simpleGrid.mapRows(RowFilter.ANY_ROW, countingChangeMapper, Optional.of(incomplete));

        // the first two rows were computed by the first mapper
        Assert.assertEquals(complete.get(0L), new IndexedData<>(0L, "b_concat"));
        Assert.assertEquals(complete.get(1L), new IndexedData<>(1L, "1_concat"));
        // the last two are computed by the last mapper
        Assert.assertEquals(complete.get(2L), new IndexedData<>(2L, "true_concat_v2"));
        Assert.assertEquals(complete.get(3L), new IndexedData<>(3L, "123123123123_concat_v2"));
    }

    protected static RowFilter firstTwoRows = new RowFilter() {

        @Override
        public boolean filterRow(long rowIndex, Row row) {
            return rowIndex < 2L;
        }
    };

    protected static RowChangeDataProducer<String> countingChangeMapper = new RowChangeDataProducer<String>() {

        @Override
        public String call(long rowId, Row row) {
            if (rowId < 2L) {
                throw new IllegalStateException("mapper called for a row that was meant to be already computed");
            }
            return concatChangeMapper.call(rowId, row) + "_v2";
        }

        @Override
        public List<String> callRowBatch(List<IndexedRow> rows) {
            if (rows.isEmpty()) {
                throw new IllegalStateException("Row mapper called on an empty batch");
            }
            return RowChangeDataProducer.super.callRowBatch(rows);
        }
    };

    public static RowChangeDataProducer<String> batchedChangeMapper = new RowChangeDataProducer<String>() {

        private static final long serialVersionUID = -2137895769820170019L;

        @Override
        public List<String> callRowBatch(List<IndexedRow> rows) {
            String val = "";
            List<String> results = new ArrayList<>();
            for (IndexedRow ir : rows) {
                val = val + "," + ir.getRow().getCellValue(1).toString();
                results.add(val);
            }
            return results;
        }

        @Override
        public int getBatchSize() {
            return 2;
        }

        @Override
        public String call(long rowId, Row row) {
            throw new NotImplementedException();
        }

    };

    @Test
    public void testGenerateBatchedChangeData() {
        ChangeData<String> changeData = simpleGrid.mapRows(myRowFilter, batchedChangeMapper, Optional.empty());

        Assert.assertEquals(changeData.get(0L), new IndexedData<>(0L, ",b"));
        Assert.assertEquals(changeData.get(1L), new IndexedData<>(1L, null)); // because it is excluded by the facet
    }

    public static RowChangeDataProducer<String> faultyBatchedChangeMapper = new RowChangeDataProducer<String>() {

        private static final long serialVersionUID = -2137895769820170019L;

        @Override
        public List<String> callRowBatch(List<IndexedRow> rows) {
            // it is incorrect to return a list of a different size than the argument
            return Collections.emptyList();
        }

        @Override
        public int getBatchSize() {
            return 2;
        }

        @Override
        public String call(long rowId, Row row) {
            throw new NotImplementedException();
        }

    };

    // TODO: require a more specific exception
    @Test(expectedExceptions = Exception.class)
    public void testGenerateFaultyRowChangeData() {
        ChangeData<String> changeData = simpleGrid.mapRows(RowFilter.ANY_ROW, faultyBatchedChangeMapper, Optional.empty());
        changeData.get(1L);
    }

    public static RowChangeDataJoiner<String> joiner = new RowChangeDataJoiner<>() {

        private static final long serialVersionUID = -21382677502256432L;

        @Override
        public Row call(Row row, IndexedData<String> indexedData) {
            return row.withCell(1, new Cell(indexedData.getData(), null, indexedData.isPending()));
        }

        @Override
        public boolean preservesRecordStructure() {
            return true;
        }

    };

    public static RecordChangeDataProducer<String> recordChangeMapper = new RecordChangeDataProducer<String>() {

        private static final long serialVersionUID = -3973242967552705600L;

        @Override
        public String call(Record record) {
            StringBuilder builder = new StringBuilder();
            for (Row row : record.getRows()) {
                builder.append(row.getCellValue(1).toString());
            }
            return builder.toString();
        }

    };

    @Test
    public void testGenerateRecordChangeData() {
        ChangeData<String> changeData = simpleGrid.mapRecords(RecordFilter.ANY_RECORD, recordChangeMapper, Optional.empty());

        Assert.assertEquals(changeData.get(0L), new IndexedData<>(0L, "b1"));
        Assert.assertEquals(changeData.get(1L), new IndexedData<>(1L, null)); // because it is not a record start
                                                                              // position
        Assert.assertEquals(changeData.get(2L), new IndexedData<>(2L, "true123123123123"));
    }

    protected static RecordFilter firstRecord = new RecordFilter() {

        @Override
        public boolean filterRecord(Record record) {
            return record.getStartRowId() == 0;
        }
    };

    protected static RecordChangeDataProducer<String> countingRecordChangeMapper = new RecordChangeDataProducer<String>() {

        @Override
        public String call(Record record) {
            if (record.getStartRowId() == 0) {
                throw new IllegalArgumentException("calling the countingRecordChangeMapper on a previously computed record");
            }
            return recordChangeMapper.call(record) + "_v2";
        }

        @Override
        public List<String> callRecordBatch(List<Record> records) {
            if (records.isEmpty()) {
                throw new IllegalStateException("calling the record mapper on an empty batch of records");
            }
            return RecordChangeDataProducer.super.callRecordBatch(records);
        }
    };

    @Test
    public void testComputeChangeDataFromIncompleteStateInRecordsMode() throws IOException, InterruptedException {

        ChangeData<String> incomplete = simpleGrid.mapRecords(firstRecord, recordChangeMapper, Optional.empty());
        File tempFile = TestUtils.createTempDirectory("test_record_based_change_data_from_incomplete");
        incomplete.saveToFile(tempFile, stringSerializer);

        // artificially pretend that the change data is incomplete by removing the completion marker
        File completionMarker = new File(tempFile, Runner.COMPLETION_MARKER_FILE_NAME);
        completionMarker.delete();

        // reload the now properly incomplete change data
        incomplete = SUT.loadChangeData(tempFile, stringSerializer);

        // complete it with more records

        ChangeData<String> complete = simpleGrid.mapRecords(RecordFilter.ANY_RECORD, countingRecordChangeMapper, Optional.of(incomplete));

        // the first record was computed by the first mapper
        Assert.assertEquals(complete.get(0L), new IndexedData<>(0L, "b1"));
        // the second by the last mapper
        Assert.assertEquals(complete.get(2L), new IndexedData(2L, "true123123123123_v2"));
    }

    public static RecordChangeDataProducer<String> faultyBatchedRecordChangeMapper = new RecordChangeDataProducer<String>() {

        private static final long serialVersionUID = -2137895769820170019L;

        @Override
        public List<String> callRecordBatch(List<Record> records) {
            // it is incorrect to return a list of a different size than the argument
            return Collections.emptyList();
        }

        @Override
        public int getBatchSize() {
            return 2;
        }

        @Override
        public String call(Record record) {
            throw new NotImplementedException();
        }

    };

    // TODO: require a more specific exception
    @Test(expectedExceptions = Exception.class)
    public void testGenerateFaultyRecordChangeData() {
        simpleGrid.mapRecords(RecordFilter.ANY_RECORD, faultyBatchedRecordChangeMapper, Optional.empty()).get(0L);
    }

    @Test
    public void testJoinChangeData() {
        Grid joined = simpleGrid.join(simpleChangeData, joiner, simpleGrid.getColumnModel());

        Grid expected = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "first" },
                        { "", null },
                        { "c", "third" },
                        { null, null }
                });

        Assert.assertEquals(joined.getColumnModel(), expected.getColumnModel());
        Assert.assertEquals(joined.collectRows(), expected.collectRows());
    }

    public static RowChangeDataFlatJoiner<String> flatJoiner = new RowChangeDataFlatJoiner<String>() {

        private static final long serialVersionUID = -60939353562371888L;

        @Override
        public List<Row> call(Row row, IndexedData<String> indexedData) {
            Row newRow = row.withCell(1, new Cell(indexedData.getData(), null, indexedData.isPending()));
            return Arrays.asList(row, newRow);
        }

    };

    @Test
    public void testFlatJoinChangeData() {
        Grid flatJoined = simpleGrid.join(simpleChangeData, flatJoiner, simpleGrid.getColumnModel());

        Grid expected = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { "a", "first" },
                        { "", 1 },
                        { "", null },
                        { "c", true },
                        { "c", "third" },
                        { null, 123123123123L },
                        { null, null }
                });

        Assert.assertEquals(flatJoined.getColumnModel(), expected.getColumnModel());
        Assert.assertEquals(flatJoined.collectRows(), expected.collectRows());
    }

    public static RecordChangeDataJoiner<String> recordJoiner = new RecordChangeDataJoiner<>() {

        private static final long serialVersionUID = -4413769252252489169L;

        @Override
        public List<Row> call(Record record, IndexedData<String> changeData) {
            return record.getRows().stream()
                    .map(row -> row.withCell(1, new Cell(changeData.getData(), null, changeData.isPending())))
                    .collect(Collectors.toList());
        }

        @Override
        public boolean preservesRecordStructure() {
            return true;
        }

    };

    @Test
    public void testRecordJoinChangeData() {
        Grid joined = simpleGrid.join(simpleChangeData, recordJoiner, simpleGrid.getColumnModel());

        Grid expected = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "first" },
                        { "", "first" },
                        { "c", "third" },
                        { null, "third" }
                });

        assertGridEquals(joined, expected);
    }

    @Test
    public void testConcatenate() {
        Grid otherGrid = createGrid(new String[] { "foo2", "bar" },
                new Serializable[][] {
                        { "k", "l" },
                        { "p", "q" }
                });

        Grid expected = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { "", 1 },
                        { "c", true },
                        { null, 123123123123L },
                        { "k", "l" },
                        { "p", "q" }
                });

        Grid concatenated = simpleGrid.concatenate(otherGrid);

        Assert.assertEquals(concatenated.getColumnModel(), expected.getColumnModel());
        Assert.assertEquals(concatenated.collectRows(), expected.collectRows());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testConcatenateIncompatibleNumberOfColumns() {
        Grid otherGrid = createGrid(new String[] { "foo2" },
                new Serializable[][] {
                        { "k" },
                        { "p" }
                });

        simpleGrid.concatenate(otherGrid);
    }

    @Test
    public void testLoadTextFile() throws IOException {
        File tempFile = new File(tempDir, "textfile.txt");
        createTestTextFile(tempFile, "foo\nbar\nbaz");

        Grid textGrid = SUT.loadTextFile(tempFile.getAbsolutePath(), mock(MultiFileReadingProgress.class), utf8);

        Grid expected = createGrid(new String[] { "Column" },
                new Serializable[][] {
                        { "foo" },
                        { "bar" },
                        { "baz" }
                });
        Assert.assertEquals(textGrid.getColumnModel(), expected.getColumnModel());
        Assert.assertEquals(textGrid.collectRows(), expected.collectRows());
    }

    @Test
    public void testLoadTextFileWithProgress() throws IOException {
        File tempFile = new File(tempDir, "textfile.txt");
        createTestTextFile(tempFile, "foo\nbar\nbaz\nhello\nworld\nwelcome\nto\nopenrefine");

        MultiFileReadingProgressStub progress = new MultiFileReadingProgressStub();
        Grid textGrid = SUT.loadTextFile(tempFile.getAbsolutePath(), progress, utf8);

        // read the whole file
        textGrid.collectRows();
        if (SUT.supportsProgressReporting()) {
            // Depending on the implementation, at least one pass was done on the whole file
            // so the progress must be at least the number of bytes in the file
            Assert.assertTrue(progress.bytesRead >= 35);
        }
    }

    @Test
    public void testLoadTextFileLimit() throws IOException {
        File tempFile = new File(tempDir, "longtextfile.txt");
        createTestTextFile(tempFile, "foo\nbar\nbaz\nhello\nworld\nwelcome\nto\nopenrefine");

        Grid textGrid = SUT.loadTextFile(tempFile.getAbsolutePath(), mock(MultiFileReadingProgress.class), utf8, 7);

        Grid expected = createGrid(new String[] { "Column" },
                new Serializable[][] {
                        { "foo" },
                        { "bar" },
                        { "baz" },
                        { "hello" },
                        { "world" },
                        { "welcome" },
                        { "to" }
                });
        Assert.assertEquals(textGrid.getColumnModel(), expected.getColumnModel());
        Assert.assertEquals(textGrid.collectRows(), expected.collectRows());
    }

    @Test
    public void testLoadTextFileTrailingNewLine() throws IOException {
        File tempFile = new File(tempDir, "textfileWithNewline.txt");
        createTestTextFile(tempFile, "foo\nbar\nbaz\n");

        Grid textGrid = SUT.loadTextFile(tempFile.getAbsolutePath(), mock(MultiFileReadingProgress.class), utf8);

        Grid expected = createGrid(new String[] { "Column" },
                new Serializable[][] {
                        { "foo" },
                        { "bar" },
                        { "baz" }
                });
        Assert.assertEquals(textGrid.getColumnModel(), expected.getColumnModel());
        Assert.assertEquals(textGrid.collectRows(), expected.collectRows());
    }

    @Test(expectedExceptions = IOException.class)
    public void testLoadTextFileDoesNotExist() throws IOException {
        SUT.loadTextFile(new File(tempDir, "doesNotExist.txt").getAbsolutePath(), mock(MultiFileReadingProgress.class), utf8);
    }

    protected void createTestTextFile(File file, String contents) throws IOException {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            fileWriter.write(contents);
        } finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
    }

    @Test
    public void testCachingWithoutProgress() {
        Assert.assertFalse(simpleGrid.isCached());
        simpleGrid.cache();
        Assert.assertTrue(simpleGrid.isCached());
        simpleGrid.uncache();
        Assert.assertFalse(simpleGrid.isCached());
    }

    @Test
    public void testCachingAsync() throws ExecutionException, InterruptedException {
        ProgressReporterStub reporter = new ProgressReporterStub();
        Assert.assertFalse(simpleGrid.isCached());
        ProgressingFuture<Boolean> future = simpleGrid.cacheAsync();
        future.onProgress(reporter);
        future.get();
        Assert.assertTrue(simpleGrid.isCached());
        Assert.assertEquals(reporter.getPercentage(), 100);
    }

    @Test
    public void testLoadGridFromIterable() throws IOException {
        CloseableIterable<Row> iterable = CloseableIterable.of(simpleGrid
                .collectRows()
                .stream()
                .map(IndexedRow::getRow)
                .collect(Collectors.toList()));

        Grid grid = getDatamodelRunner()
                .gridFromIterable(simpleGrid.getColumnModel(), iterable, Collections.emptyMap(), -1L, -1L);

        assertGridEquals(grid, simpleGrid);
    }

    @Test
    public void testLoadChangeDataFromIterable() throws IOException {
        List<IndexedData<String>> indexedData = Arrays.asList(new IndexedData<String>(0L, "first"),
                new IndexedData<String>(2L, "third"),
                new IndexedData<String>(3L, null));
        CloseableIterable<IndexedData<String>> iterable = CloseableIterable.of(indexedData);

        ChangeData<String> changeData = getDatamodelRunner()
                .changeDataFromIterable(iterable, -1L);

        Assert.assertEquals(changeData.get(0L), new IndexedData<>(0L, "first"));
    }

    @Test
    public void testEmptyChangeData() throws IOException {
        ChangeData<String> changeData = getDatamodelRunner().emptyChangeData();

        Assert.assertFalse(changeData.isComplete());
        Assert.assertEquals(changeData.get(3L), new IndexedData<String>(3L));
    }

    /**
     * Because {@link Grid} implementations are not required to use the {@link Object#equals(Object)} method to compare
     * the contents of grids, we use this helper to check that two grids have the same contents.
     *
     * @param actual
     * @param expected
     */
    public static void assertGridEquals(Grid actual, Grid expected) {
        Assert.assertEquals(actual.getColumnModel(), expected.getColumnModel());
        Assert.assertEquals(actual.collectRows(), expected.collectRows());
        Assert.assertEquals(actual.getOverlayModels(), expected.getOverlayModels());
    }
}
