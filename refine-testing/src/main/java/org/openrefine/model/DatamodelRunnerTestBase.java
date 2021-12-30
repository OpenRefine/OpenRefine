
package org.openrefine.model;

import static org.mockito.Mockito.mock;

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
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.NotImplementedException;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

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
import org.openrefine.model.GridState.ApproxCount;
import org.openrefine.model.GridState.PartialAggregation;
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
import org.openrefine.sorting.NumberCriterion;
import org.openrefine.sorting.SortingConfig;
import org.openrefine.sorting.StringCriterion;
import org.openrefine.util.TestUtils;

/**
 * A collection of generic tests that any implementation of {@link DatamodelRunner} should satisfy.
 * 
 * These tests are provided in this module so that other implementations can reuse this test class.
 * 
 * @author Antonin Delpeuch
 *
 */
public abstract class DatamodelRunnerTestBase {

    protected DatamodelRunner SUT;

    protected GridState simpleGrid, longerGrid, gridToSort;
    protected ChangeData<String> simpleChangeData;
    protected List<Row> expectedRows;
    protected List<Record> expectedRecords;
    protected SortingConfig sortingConfig;
    protected OverlayModel overlayModel;
    protected Charset utf8 = Charset.forName("UTF-8");

    protected File tempDir;

    public abstract DatamodelRunner getDatamodelRunner() throws IOException;

    @BeforeTest
    public void setUp() throws IOException {
        SUT = getDatamodelRunner();
        tempDir = TestUtils.createTempDirectory("datamodelrunnertest");
    }

    @AfterTest
    public void tearDown() {
        SUT = null;
        try {
            FileUtils.deleteDirectory(tempDir);
        } catch (IOException e) {
            ;
        }
        tempDir = null;
    }

    protected GridState createGrid(String[] columnNames, Cell[][] cells) {
        ColumnModel cm = new ColumnModel(Arrays.asList(columnNames)
                .stream()
                .map(name -> new ColumnMetadata(name))
                .collect(Collectors.toList()));
        List<Row> rows = new ArrayList<>(cells.length);
        for (int i = 0; i != cells.length; i++) {
            rows.add(new Row(Arrays.asList(cells[i])));
        }
        return SUT.create(cm, rows, Collections.emptyMap());
    }

    protected GridState createGrid(String[] columnNames, Serializable[][] cellValues) {
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
        return SUT.create(Arrays.asList(data));
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
        GridState withOverlayModel = simpleGrid.withOverlayModels(Collections.singletonMap("foo", overlayModel));
        Map<String, OverlayModel> overlayModels = withOverlayModel.getOverlayModels();
        Assert.assertEquals(overlayModels.get("foo"), overlayModel);
        Assert.assertNull(overlayModels.get("bar"));
    }

    @Test
    public void testDatamodelRunner() {
        Assert.assertNotNull(simpleGrid.getDatamodelRunner());
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
        GridState state = simpleGrid;

        Assert.assertEquals(state.rowCount(), 4L);

        Assert.assertEquals(state.getRow(0L), expectedRows.get(0));
        Assert.assertEquals(state.getRow(3L), expectedRows.get(3));
        try {
            state.getRow(4L);
            Assert.fail("No exception thrown by an out of bounds access");
        } catch (IndexOutOfBoundsException e) {
            ;
        }

        Assert.assertEquals(
                state.getRows(1L, 2).stream().map(ir -> ir.getRow()).collect(Collectors.toList()),
                expectedRows.subList(1, 3));
        Assert.assertEquals(state.getRows(5L, 3), Collections.emptyList());

        Assert.assertEquals(state.getRows(myRowFilter, SortingConfig.NO_SORTING, 0L, 2),
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
    public void testAccessSortedRows() {
        GridState state = gridToSort;

        Assert.assertEquals(
                state.getRows(RowFilter.ANY_ROW, sortingConfig, 0, 2),
                Arrays.asList(
                        new IndexedRow(2L, row(null, 0)),
                        new IndexedRow(1L, row("a", 1))));

        Assert.assertEquals(
                state.getRows(RowFilter.ANY_ROW, sortingConfig, 2, 2),
                Arrays.asList(
                        new IndexedRow(0L, row("c", 1)),
                        new IndexedRow(3L, row("a", 5))));
    }

    @Test
    public void testAccessSortedRowsOutOfBounds() {
        GridState state = gridToSort;

        Assert.assertEquals(
                state.getRows(RowFilter.ANY_ROW, sortingConfig, 30, 10),
                Collections.emptyList());
    }

    protected static RecordFilter myRecordFilter = new RecordFilter() {

        private static final long serialVersionUID = 4197928472022711691L;

        @Override
        public boolean filterRecord(Record record) {
            return record.getStartRowId() == 2L;
        }
    };

    @Test
    public void testIterateRowsFilter() {
        Iterator<IndexedRow> indexedRows = simpleGrid.iterateRows(myRowFilter, SortingConfig.NO_SORTING).iterator();
        Assert.assertTrue(indexedRows.hasNext());
        Assert.assertEquals(indexedRows.next(), new IndexedRow(0L, expectedRows.get(0)));
        Assert.assertTrue(indexedRows.hasNext());
        Assert.assertEquals(indexedRows.next(), new IndexedRow(2L, expectedRows.get(2)));
        Assert.assertTrue(indexedRows.hasNext());
    }

    @Test
    public void testIterateRowsSortingConfig() {
        Iterator<IndexedRow> indexedRows = gridToSort.iterateRows(RowFilter.ANY_ROW, sortingConfig).iterator();
        Assert.assertTrue(indexedRows.hasNext());
        Assert.assertEquals(indexedRows.next(), new IndexedRow(2L, row(null, 0)));
        Assert.assertTrue(indexedRows.hasNext());
        Assert.assertEquals(indexedRows.next(), new IndexedRow(1L, row("a", 1)));
        Assert.assertTrue(indexedRows.hasNext());
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
        // it can also filter slightly less than the limit
        ApproxCount count = simpleGrid.countMatchingRecordsApprox(myRecordFilter, 2);
        Assert.assertTrue(count.getMatched() <= count.getProcessed());
        Assert.assertTrue(count.getProcessed() <= 2);
    }

    @Test
    public void testAccessRecords() {
        GridState state = simpleGrid;

        Assert.assertEquals(state.recordCount(), 2L);

        Assert.assertEquals(state.getRecord(0L), expectedRecords.get(0));
        Assert.assertEquals(state.getRecord(2L), expectedRecords.get(1));
        try {
            state.getRecord(1L);
            Assert.fail("No exception thrown by an out of bounds access");
        } catch (IllegalArgumentException e) {
            ;
        }

        Assert.assertEquals(state.getRecords(1L, 2), expectedRecords.subList(1, 2));

        Assert.assertEquals(state.getRecords(myRecordFilter, SortingConfig.NO_SORTING, 0L, 3),
                Collections.singletonList(expectedRecords.get(1)));
    }

    @Test
    public void testRecordGroupingNoRecordStart() {
        GridState noRecordStart = createGrid(new String[] { "foo", "bar" },
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
    public void testAccessSortedRecords() {
        GridState state = gridToSort;

        Assert.assertEquals(
                state.getRecords(RecordFilter.ANY_RECORD, sortingConfig, 0, 3),
                Arrays.asList(
                        new Record(1L, Arrays.asList(row("a", 1), row(null, 0))),
                        new Record(0L, Arrays.asList(row("c", 1))),
                        new Record(3L, Arrays.asList(row("a", 5)))));

    }

    @Test
    public void testRecordsRespectKeyColumnIndex() {
        GridState state = simpleGrid.withColumnModel(simpleGrid.getColumnModel().withKeyColumnIndex(1));

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
        Iterator<Record> records = simpleGrid.iterateRecords(myRecordFilter, SortingConfig.NO_SORTING).iterator();
        Assert.assertTrue(records.hasNext());
        Assert.assertEquals(records.next(), expectedRecords.get(1));
        Assert.assertFalse(records.hasNext());
    }

    @Test
    public void testIterateRecordsSortingConfig() {
        Iterator<Record> records = gridToSort.iterateRecords(RecordFilter.ANY_RECORD, sortingConfig).iterator();
        Assert.assertTrue(records.hasNext());
        Assert.assertEquals(records.next(), new Record(1L, Arrays.asList(row("a", 1), row(null, 0))));
        Assert.assertTrue(records.hasNext());
        Assert.assertEquals(records.next(), new Record(0L, Arrays.asList(row("c", 1))));
        Assert.assertTrue(records.hasNext());
        Assert.assertEquals(records.next(), new Record(3L, Arrays.asList(row("a", 5))));
        Assert.assertFalse(records.hasNext());
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
        Assert.assertTrue(count.getProcessed() <= 2);
    }

    @Test
    public void testRoundTripSerializationNoProgress() throws IOException, InterruptedException {
        File tempFile = new File(tempDir, "testgrid_no_progress");

        simpleGrid.saveToFile(tempFile);

        GridState loaded = SUT.loadGridState(tempFile);

        Assert.assertEquals(loaded.rowCount(), 4L);
        List<Row> actualRows = loaded.collectRows().stream().map(r -> r.getRow()).collect(Collectors.toList());
        Assert.assertEquals(actualRows, expectedRows);
        Assert.assertEquals(loaded.recordCount(), 2L);
        Assert.assertEquals(loaded.collectRecords(), expectedRecords);
    }

    @Test
    public void testRoundTripSerializationWithProgress() throws IOException, InterruptedException {
        File tempFile = new File(tempDir, "testgrid_progress");
        ProgressReporterStub reporter = new ProgressReporterStub();

        simpleGrid.saveToFile(tempFile, reporter);
        if (SUT.supportsProgressReporting()) {
            Assert.assertEquals(reporter.getPercentage(), 100);
        }

        GridState loaded = SUT.loadGridState(tempFile);

        Assert.assertEquals(loaded.rowCount(), 4L);
        List<Row> actualRows = loaded.collectRows().stream().map(r -> r.getRow()).collect(Collectors.toList());
        Assert.assertEquals(actualRows, expectedRows);
        Assert.assertEquals(loaded.recordCount(), 2L);
        Assert.assertEquals(loaded.collectRecords(), expectedRecords);
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
        GridState grid = SUT.create(columnModel, rows, Collections.emptyMap());

        File tempFile = new File(tempDir, "testgrid_recon");
        grid.saveToFile(tempFile);

        GridState loaded = SUT.loadGridState(tempFile);
        Assert.assertEquals(loaded.collectRows(), grid.collectRows());
    }

    @Test
    public void testComputeRowFacets() {
        Facet facetFoo = new StringFacet.Config("foo", "a").apply(simpleGrid.getColumnModel());
        Facet facetBar = new StringFacet.Config("bar", null).apply(simpleGrid.getColumnModel());

        List<Facet> facets = Arrays.asList(facetFoo, facetBar);
        List<FacetState> initialStates = facets
                .stream().map(facet -> facet.getInitialFacetState())
                .collect(Collectors.toList());

        AllFacetsAggregator aggregator = new AllFacetsAggregator(facets
                .stream().map(facet -> facet.getAggregator())
                .collect(Collectors.toList()));

        AllFacetsState states = simpleGrid.aggregateRows(aggregator, new AllFacetsState(ImmutableList.copyOf(initialStates), 0L, 0L));

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
        Facet facetFoo = new StringFacet.Config("foo", "a").apply(simpleGrid.getColumnModel());
        Facet facetBar = new StringFacet.Config("bar", null).apply(simpleGrid.getColumnModel());

        List<Facet> facets = Arrays.asList(facetFoo, facetBar);
        List<FacetState> initialStates = facets
                .stream().map(facet -> facet.getInitialFacetState())
                .collect(Collectors.toList());

        AllFacetsAggregator aggregator = new AllFacetsAggregator(facets
                .stream().map(facet -> facet.getAggregator())
                .collect(Collectors.toList()));

        AllFacetsState states = simpleGrid.aggregateRecords(aggregator, new AllFacetsState(ImmutableList.copyOf(initialStates), 0L, 0L));

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
        Assert.assertTrue(partialResult.getState().value <= 2L);
        Assert.assertTrue(partialResult.limitReached());
        Assert.assertTrue(partialResult.getProcessed() <= 2L);
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
        Assert.assertTrue(partialResult.getState().value <= 2L);
        Assert.assertTrue(partialResult.limitReached());
        Assert.assertTrue(partialResult.getProcessed() <= 2L);
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

    };

    @Test
    public void testMapRows() {
        GridState mapped = simpleGrid.mapRows(
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
        GridState mapped = simpleGrid.flatMapRows(
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
        GridState mapped = longerGrid.mapRows(
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
        GridState mapped = simpleGrid.mapRecords(
                concatRecordMapper, simpleGrid.getColumnModel());

        List<IndexedRow> rows = mapped.collectRows();
        Assert.assertEquals(rows.get(0).getRow().getCellValue(1), "b_concat");
        Assert.assertEquals(rows.get(1).getRow().getCellValue(1), "1_concat");
        Assert.assertEquals(rows.get(2).getRow().getCellValue(1), "true_concat");
    }

    @Test
    public void testReorderRows() {
        GridState reordered = gridToSort.reorderRows(sortingConfig);

        GridState expected = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { null, 0 },
                        { "a", 1 },
                        { "c", 1 },
                        { "a", 5 }
                });

        Assert.assertEquals(reordered.collectRows(), expected.collectRows());
    }

    @Test
    public void testReorderRecords() {
        GridState reordered = gridToSort.reorderRecords(sortingConfig);

        GridState expected = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", 1 },
                        { null, 0 },
                        { "c", 1 },
                        { "a", 5 }
                });

        Assert.assertEquals(reordered.collectRows(), expected.collectRows());
    }

    @Test
    public void testRemoveRows() {
        GridState removed = simpleGrid.removeRows(myRowFilter);

        GridState expected = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "", 1 }
                });

        Assert.assertEquals(removed.getColumnModel(), expected.getColumnModel());
        Assert.assertEquals(removed.collectRows(), expected.collectRows());
    }

    @Test
    public void testRemoveRecords() {
        GridState removed = simpleGrid.removeRecords(myRecordFilter);

        GridState expected = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { "", 1 }
                });

        Assert.assertEquals(removed.getColumnModel(), expected.getColumnModel());
        Assert.assertEquals(removed.collectRows(), expected.collectRows());
    }

    @Test
    public void testLimitRows() {
        GridState limited = simpleGrid.limitRows(2L);
        GridState expected = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { "", 1 }
                });

        Assert.assertEquals(limited.getColumnModel(), expected.getColumnModel());
        Assert.assertEquals(limited.collectRows(), expected.collectRows());
    }

    @Test
    public void testDropRows() {
        GridState limited = simpleGrid.dropRows(2L);
        GridState expected = createGrid(new String[] { "foo", "bar" },
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

    @Test
    public void testSerializeChangeDataNoProgress() throws IOException, InterruptedException {
        File tempFile = new File(tempDir, "test_change_data");

        simpleChangeData.saveToFile(new File(tempFile, "data"), stringSerializer);

        ChangeData<String> loaded = SUT.loadChangeData(new File(tempFile, "data"), stringSerializer);

        Assert.assertNotNull(loaded.getDatamodelRunner());
        Assert.assertEquals(loaded.get(0L), "first");
        Assert.assertNull(loaded.get(1L)); // not included in changedata
        Assert.assertEquals(loaded.get(2L), "third");
        Assert.assertNull(loaded.get(3L)); // null from creation
    }

    @Test
    public void testSerializeChangeDataWithProgress() throws IOException, InterruptedException {
        File tempFile = new File(tempDir, "test_change_data_with_progress");
        ProgressReporterStub progress = new ProgressReporterStub();

        simpleChangeData.saveToFile(new File(tempFile, "data"), stringSerializer, progress);
        Assert.assertEquals(progress.getPercentage(), 100);

        ChangeData<String> loaded = SUT.loadChangeData(new File(tempFile, "data"), stringSerializer);

        Assert.assertNotNull(loaded.getDatamodelRunner());
        Assert.assertEquals(loaded.get(0L), "first");
        Assert.assertNull(loaded.get(1L)); // not included in changedata
        Assert.assertEquals(loaded.get(2L), "third");
        Assert.assertNull(loaded.get(3L)); // null from creation
    }

    @Test
    public void testSerializeChangeDataDirAlreadyExists() throws IOException, InterruptedException {
        File tempFile = TestUtils.createTempDirectory("test_change_data_already_exists");

        simpleChangeData.saveToFile(tempFile, stringSerializer);

        ChangeData<String> loaded = SUT.loadChangeData(tempFile, stringSerializer);

        Assert.assertNotNull(loaded.getDatamodelRunner());
        Assert.assertEquals(loaded.get(0L), "first");
        Assert.assertNull(loaded.get(1L));
        Assert.assertEquals(loaded.get(2L), "third");
        Assert.assertNull(loaded.get(3L));
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
        ChangeData<String> changeData = simpleGrid.mapRows(myRowFilter, concatChangeMapper);

        Assert.assertEquals(changeData.get(0L), "b_concat");
        Assert.assertNull(changeData.get(1L)); // because it is excluded by the facet
    }

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
        ChangeData<String> changeData = simpleGrid.mapRows(myRowFilter, batchedChangeMapper);

        Assert.assertEquals(changeData.get(0L), ",b");
        Assert.assertNull(changeData.get(1L)); // because it is excluded by the facet
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
        ChangeData<String> changeData = simpleGrid.mapRows(RowFilter.ANY_ROW, faultyBatchedChangeMapper);
        changeData.get(1L);
    }

    public static RowChangeDataJoiner<String> joiner = new RowChangeDataJoiner<String>() {

        private static final long serialVersionUID = -21382677502256432L;

        @Override
        public Row call(long rowId, Row row, String changeData) {
            return row.withCell(1, new Cell(changeData, null));
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
        ChangeData<String> changeData = simpleGrid.mapRecords(RecordFilter.ANY_RECORD, recordChangeMapper);

        Assert.assertEquals(changeData.get(0L), "b1");
        Assert.assertNull(changeData.get(1L)); // because it is not a record start position
        Assert.assertEquals(changeData.get(2L), "true123123123123");
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
        simpleGrid.mapRecords(RecordFilter.ANY_RECORD, faultyBatchedRecordChangeMapper).get(0L);
    }

    @Test
    public void testJoinChangeData() {
        GridState joined = simpleGrid.join(simpleChangeData, joiner, simpleGrid.getColumnModel());

        GridState expected = createGrid(new String[] { "foo", "bar" },
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
        public List<Row> call(long rowId, Row row, String changeData) {
            Row newRow = row.withCell(1, new Cell(changeData, null));
            return Arrays.asList(row, newRow);
        }

    };

    @Test
    public void testFlatJoinChangeData() {
        GridState flatJoined = simpleGrid.join(simpleChangeData, flatJoiner, simpleGrid.getColumnModel());

        GridState expected = createGrid(new String[] { "foo", "bar" },
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

    public static RecordChangeDataJoiner<String> recordJoiner = new RecordChangeDataJoiner<String>() {

        private static final long serialVersionUID = -4413769252252489169L;

        @Override
        public List<Row> call(Record record, String changeData) {
            return record.getRows().stream().map(row -> row.withCell(1, new Cell(changeData, null))).collect(Collectors.toList());
        }

    };

    @Test
    public void testRecordJoinChangeData() {
        GridState joined = simpleGrid.join(simpleChangeData, recordJoiner, simpleGrid.getColumnModel());

        GridState expected = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "first" },
                        { "", "first" },
                        { "c", "third" },
                        { null, "third" }
                });

        Assert.assertEquals(joined.getColumnModel(), expected.getColumnModel());
        Assert.assertEquals(joined.collectRows(), expected.collectRows());
    }

    @Test
    public void testConcatenate() {
        GridState otherGrid = createGrid(new String[] { "foo2", "bar" },
                new Serializable[][] {
                        { "k", "l" },
                        { "p", "q" }
                });

        GridState expected = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { "", 1 },
                        { "c", true },
                        { null, 123123123123L },
                        { "k", "l" },
                        { "p", "q" }
                });

        GridState concatenated = simpleGrid.concatenate(otherGrid);

        Assert.assertEquals(concatenated.getColumnModel(), expected.getColumnModel());
        Assert.assertEquals(concatenated.collectRows(), expected.collectRows());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testConcatenateIncompatibleNumberOfColumns() {
        GridState otherGrid = createGrid(new String[] { "foo2" },
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

        GridState textGrid = SUT.loadTextFile(tempFile.getAbsolutePath(), mock(MultiFileReadingProgress.class), utf8);

        GridState expected = createGrid(new String[] { "Column" },
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
        GridState textGrid = SUT.loadTextFile(tempFile.getAbsolutePath(), progress, utf8);

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

        GridState textGrid = SUT.loadTextFile(tempFile.getAbsolutePath(), mock(MultiFileReadingProgress.class), utf8, 7);

        GridState expected = createGrid(new String[] { "Column" },
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

        GridState textGrid = SUT.loadTextFile(tempFile.getAbsolutePath(), mock(MultiFileReadingProgress.class), utf8);

        GridState expected = createGrid(new String[] { "Column" },
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
    public void testCachingWithProgress() {
        ProgressReporterStub reporter = new ProgressReporterStub();
        Assert.assertFalse(simpleGrid.isCached());
        simpleGrid.cache(reporter);
        Assert.assertTrue(simpleGrid.isCached());
        Assert.assertEquals(reporter.getPercentage(), 100);
    }
}
