
package org.openrefine.model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.browsing.facets.AllFacetsAggregator;
import org.openrefine.browsing.facets.Facet;
import org.openrefine.browsing.facets.FacetResult;
import org.openrefine.browsing.facets.FacetState;
import org.openrefine.browsing.facets.StringFacet;
import org.openrefine.browsing.facets.StringFacetState;
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

    protected GridState simpleGrid;
    protected List<Row> expectedRows;
    protected List<Record> expectedRecords;

    public abstract DatamodelRunner getDatamodelRunner();

    @BeforeTest
    public void setUpDatamodelRunner() {
        SUT = getDatamodelRunner();
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

    @BeforeMethod
    public void setUpSimpleGrid() {
        simpleGrid = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { "", 1 },
                        { "c", true },
                        { null, 123123123123L }
                });
        expectedRows = Arrays.asList(
                new Row(Arrays.asList(new Cell("a", null), new Cell("b", null))),
                new Row(Arrays.asList(new Cell("", null), new Cell(1, null))),
                new Row(Arrays.asList(new Cell("c", null), new Cell(true, null))),
                new Row(Arrays.asList(null, new Cell(123123123123L, null))));
        expectedRecords = Arrays.asList(
                new Record(0L, Arrays.asList(
                        expectedRows.get(0),
                        expectedRows.get(1))),
                new Record(2L, Arrays.asList(
                        expectedRows.get(2),
                        expectedRows.get(3))));
    }

    @Test
    public void testAccessMetadata() {
        Assert.assertEquals(simpleGrid.getColumnModel(),
                new ColumnModel(Arrays.asList(new ColumnMetadata("foo"), new ColumnMetadata("bar"))));
        Assert.assertEquals(simpleGrid.getOverlayModels(), Collections.emptyMap());
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

        Assert.assertEquals(state.getRows(myRowFilter, 0L, 2),
                Arrays.asList(new IndexedRow(0L, expectedRows.get(0)),
                        new IndexedRow(2L, expectedRows.get(2))));
    }

    protected static RecordFilter myRecordFilter = new RecordFilter() {

        private static final long serialVersionUID = 4197928472022711691L;

        @Override
        public boolean filterRecord(Record record) {
            return record.getStartRowId() == 2L;
        }
    };

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

        Assert.assertEquals(state.getRecords(myRecordFilter, 0L, 3), Collections.singletonList(expectedRecords.get(1)));
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
    public void testRoundTripSerialization() throws IOException {
        File tempFile = TestUtils.createTempDirectory("testgrid");

        simpleGrid.saveToFile(tempFile);

        GridState loaded = SUT.loadGridState(tempFile);

        Assert.assertEquals(loaded.rowCount(), 4L);
        Assert.assertEquals(loaded.recordCount(), 2L);
        Assert.assertEquals(loaded.collectRows().stream().map(r -> r.getRow()).collect(Collectors.toList()),
                expectedRows);
        Assert.assertEquals(loaded.collectRecords(), expectedRecords);
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

        List<FacetState> states = simpleGrid.aggregateRows(aggregator, initialStates);

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

        List<FacetState> states = simpleGrid.aggregateRecords(aggregator, initialStates);

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
}
