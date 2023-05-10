/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package org.openrefine.browsing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.facets.AllFacetsState;
import org.openrefine.browsing.facets.Facet;
import org.openrefine.browsing.facets.FacetAggregator;
import org.openrefine.browsing.facets.FacetAggregatorStub;
import org.openrefine.browsing.facets.FacetConfig;
import org.openrefine.browsing.facets.FacetResult;
import org.openrefine.browsing.facets.FacetStateStub;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Grid.PartialAggregation;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowInRecordFilter;
import org.openrefine.sorting.SortingConfig;
import org.openrefine.util.CloseableIterator;

public class EngineTests {

    private Engine engine;
    private Engine enginePartial;
    private Grid initialState;
    private EngineConfig engineConfig;
    private EngineConfig engineConfigPartial;
    private long projectId = 1234L;
    protected static RowInRecordFilter filterA = new RowInRecordFilter(true) {

        private static final long serialVersionUID = -609451600084843923L;

        @Override
        public boolean filterRow(long rowIndex, Row row, Record record) {
            return "a".equals(row.getCellValue(0));
        }
    };
    protected static RowInRecordFilter noFilter = RowInRecordFilter.ANY_ROW_IN_RECORD;
    private AllFacetsState allRowsState = new AllFacetsState(
            ImmutableList.of(new FacetStateStub(65, 35), new FacetStateStub(100, 0)),
            ImmutableList.of(), 100, 65);
    private AllFacetsState partialState = new AllFacetsState(
            ImmutableList.of(new FacetStateStub(8, 2), new FacetStateStub(10, 0)),
            ImmutableList.of(), 10, 8);
    private PartialAggregation<Serializable> partialStateWrapped = new PartialAggregation<Serializable>(partialState, 10, true);

    @SuppressWarnings("unchecked")
    @BeforeMethod
    public void createInitialGrid() {
        List<Row> rows = Arrays.asList(
                new Row(Arrays.asList(new Cell("a", null), new Cell("b", null))),
                new Row(Arrays.asList(new Cell("c", null), new Cell("d", null))));
        ColumnModel columnModel = new ColumnModel(Arrays.asList(
                new ColumnMetadata("column1"),
                new ColumnMetadata("column2")));
        initialState = mock(Grid.class);
        when(initialState.getColumnModel()).thenReturn(columnModel);
        Facet facetA = mock(Facet.class);
        Facet facetAll = mock(Facet.class);
        FacetConfig facetConfigA = mock(FacetConfig.class);
        FacetConfig facetConfigAll = mock(FacetConfig.class);

        when(facetConfigA.apply(Mockito.any(ColumnModel.class), Mockito.any(Map.class), Mockito.anyLong())).thenReturn(facetA);
        when(facetConfigAll.apply(Mockito.any(ColumnModel.class), Mockito.any(Map.class), Mockito.anyLong())).thenReturn(facetAll);
        when(facetA.getInitialFacetState()).thenReturn(new FacetStateStub(0, 0));
        when(facetAll.getInitialFacetState()).thenReturn(new FacetStateStub(0, 0));
        when(facetA.getFacetResult(new FacetStateStub(65, 35))).thenReturn(new FacetStateStub(65, 35));
        when(facetAll.getFacetResult(new FacetStateStub(100, 0))).thenReturn(new FacetStateStub(100, 0));
        when(facetConfigA.isNeutral()).thenReturn(false);
        when(facetConfigAll.isNeutral()).thenReturn(true);
        when((FacetAggregator<FacetStateStub>) facetA.getAggregator()).thenReturn(new FacetAggregatorStub(filterA));
        when((FacetAggregator<FacetStateStub>) facetAll.getAggregator()).thenReturn(new FacetAggregatorStub(noFilter));
        when(initialState.aggregateRows(Mockito.any(), Mockito.any())).thenReturn(allRowsState);
        when(initialState.aggregateRowsApprox(Mockito.any(), Mockito.any(), Mockito.anyLong())).thenReturn(partialStateWrapped);
        when(initialState.rowCount()).thenReturn(123L);
        when(initialState.recordCount()).thenReturn(100L);

        List<FacetConfig> facetConfigs = Arrays.asList(
                facetConfigA, facetConfigAll);
        engineConfig = new EngineConfig(facetConfigs, Mode.RowBased);
        engineConfigPartial = new EngineConfig(facetConfigs, Mode.RowBased, 2L);
        engine = new Engine(initialState, engineConfig, 1234L);
        enginePartial = new Engine(initialState, engineConfigPartial, 1234L);
    }

    @Test
    public void testAccessors() {
        Assert.assertEquals(engine.getMode(), Mode.RowBased);
        Assert.assertEquals(engine.getConfig(), engineConfig);
        Assert.assertEquals(engine.getGrid(), initialState);
    }

    @Test
    public void testFacetStates() {
        PartialAggregation<AllFacetsState> facetStates = engine.getFacetsState();

        Assert.assertEquals(facetStates.getState(), allRowsState);
        Assert.assertEquals(facetStates.getProcessed(), 100L);
        Assert.assertFalse(facetStates.limitReached());
    }

    @Test
    public void testFacetStatesApprox() {
        PartialAggregation<AllFacetsState> facetStates = enginePartial.getFacetsState();

        Assert.assertEquals(facetStates.getState(), partialState);
        Assert.assertEquals(facetStates.getProcessed(), 10L);
        Assert.assertTrue(facetStates.limitReached());
    }

    @Test
    public void testFacetResults() {
        List<FacetResult> facetResults = engine.getFacetResults();

        Assert.assertEquals(facetResults, Arrays.asList(new FacetStateStub(65, 35), new FacetStateStub(100, 0)));
    }

    @Test
    public void testAggregationCount() {
        Assert.assertEquals(engine.getAggregatedCount(), 100);
        Assert.assertEquals(enginePartial.getAggregatedCount(), 10);
    }

    @Test
    public void testFilteredCount() {
        Assert.assertEquals(engine.getFilteredCount(), 65);
        Assert.assertEquals(enginePartial.getFilteredCount(), 8);
    }

    @Test
    public void testLimitReached() {
        Assert.assertFalse(engine.limitReached());
        Assert.assertTrue(enginePartial.limitReached());
    }

    @Test
    public void testGetTotals() {
        Assert.assertEquals(engine.getTotalRows(), 123L);
        Assert.assertEquals(enginePartial.getTotalRows(), 123L);
        Assert.assertEquals(engine.getTotalCount(), 123L);
        Assert.assertEquals(enginePartial.getTotalCount(), 123L);
    }

    @Test
    public void testNeutral() {
        Assert.assertFalse(engine.isNeutral());
        Assert.assertFalse(enginePartial.isNeutral());
    }

    @Test
    public void testGetMatchingRows() {
        @SuppressWarnings("unchecked")
        CloseableIterator<IndexedRow> mockIterator = mock(CloseableIteratorIndexedRows.class);
        when(initialState.iterateRows(Mockito.any())).thenReturn(mockIterator);

        // cast to Object to force referential equality for testing purposes
        Assert.assertEquals((Object) engine.getMatchingRows(SortingConfig.NO_SORTING), mockIterator);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testGetMatchingRecordsInRowsMode() {
        engine.getMatchingRecords(SortingConfig.NO_SORTING);
    }

    private static interface CloseableIteratorIndexedRows extends CloseableIterator<IndexedRow> {

    }
}
