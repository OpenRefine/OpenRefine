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

import java.util.Arrays;
import java.util.List;

import org.mockito.Matchers;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.facets.Facet;
import org.openrefine.browsing.facets.FacetAggregator;
import org.openrefine.browsing.facets.FacetAggregatorStub;
import org.openrefine.browsing.facets.FacetConfig;
import org.openrefine.browsing.facets.FacetStateStub;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;

public class EngineTests {

    private Engine SUT;
    private GridState initialState;
    private EngineConfig engineConfig;
    protected static RowFilter filterA = new RowFilter() {

        private static final long serialVersionUID = -609451600084843923L;

        @Override
        public boolean filterRow(long rowIndex, Row row) {
            return "a".equals(row.getCellValue(0));
        }
    };
    protected static RowFilter noFilter = new RowFilter() {

        private static final long serialVersionUID = -2414317361092526726L;

        @Override
        public boolean filterRow(long rowIndex, Row row) {
            return true;
        }
    };

    @SuppressWarnings("unchecked")
    @BeforeMethod
    public void createInitialGrid() {
        List<Row> rdd = Arrays.asList(
                new Row(Arrays.asList(new Cell("a", null), new Cell("b", null))),
                new Row(Arrays.asList(new Cell("c", null), new Cell("d", null))));
        ColumnModel columnModel = new ColumnModel(Arrays.asList(
                new ColumnMetadata("column1"),
                new ColumnMetadata("column2")));
        initialState = mock(GridState.class);
        when(initialState.getColumnModel()).thenReturn(columnModel);
        Facet facetA = mock(Facet.class);
        Facet facetAll = mock(Facet.class);
        FacetConfig facetConfigA = mock(FacetConfig.class);
        FacetConfig facetConfigAll = mock(FacetConfig.class);

        when(facetConfigA.apply(Matchers.any(ColumnModel.class))).thenReturn(facetA);
        when(facetConfigAll.apply(Matchers.any(ColumnModel.class))).thenReturn(facetAll);
        when(facetA.getInitialFacetState()).thenReturn(new FacetStateStub(0, 0));
        when(facetAll.getInitialFacetState()).thenReturn(new FacetStateStub(0, 0));
        when((FacetAggregator<FacetStateStub>) facetA.getAggregator()).thenReturn(new FacetAggregatorStub(filterA));
        when((FacetAggregator<FacetStateStub>) facetAll.getAggregator()).thenReturn(new FacetAggregatorStub(noFilter));

        List<FacetConfig> facetConfigs = Arrays.asList(
                facetConfigA, facetConfigAll);
        engineConfig = new EngineConfig(facetConfigs, Mode.RowBased);
        SUT = new Engine(initialState, engineConfig);
    }

    @Test
    public void testAccessors() {
        Assert.assertEquals(SUT.getMode(), Mode.RowBased);
        Assert.assertEquals(SUT.getConfig(), engineConfig);
        Assert.assertEquals(SUT.getGridState(), initialState);
    }

}
