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
import java.util.Collections;
import java.util.List;

import org.apache.spark.api.java.JavaPairRDD;
import org.mockito.Matchers;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import scala.Tuple2;

import org.openrefine.SparkBasedTest;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.facets.Facet;
import org.openrefine.browsing.facets.FacetConfig;
import org.openrefine.browsing.facets.FacetState;
import org.openrefine.browsing.facets.FacetStateStub;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;

public class EngineTests extends SparkBasedTest {

    private Engine SUT;
    private GridState initialState;
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

    @BeforeMethod
    public void createInitialGrid() {
        JavaPairRDD<Long, Row> rdd = rowRDD(new Cell[][] {
                { new Cell("a", null), new Cell("b", null) },
                { new Cell("c", null), new Cell("d", null) }
        });
        ColumnModel columnModel = new ColumnModel(Arrays.asList(
                new ColumnMetadata("column1"),
                new ColumnMetadata("column2")));
        initialState = new GridState(columnModel, rdd, Collections.emptyMap());
        Facet facetA = mock(Facet.class);
        Facet facetAll = mock(Facet.class);
        FacetConfig facetConfigA = mock(FacetConfig.class);
        FacetConfig facetConfigAll = mock(FacetConfig.class);

        when(facetConfigA.apply(Matchers.any(ColumnModel.class))).thenReturn(facetA);
        when(facetConfigAll.apply(Matchers.any(ColumnModel.class))).thenReturn(facetAll);
        when(facetA.getInitialFacetState()).thenReturn(new FacetStateStub(0, 0, filterA));
        when(facetAll.getInitialFacetState()).thenReturn(new FacetStateStub(0, 0, noFilter));
        when(facetA.getRowFilter()).thenReturn(filterA);
        when(facetAll.getRowFilter()).thenReturn(noFilter);

        List<FacetConfig> facetConfigs = Arrays.asList(
                facetConfigA, facetConfigAll);
        SUT = new Engine(initialState, new EngineConfig(facetConfigs, Mode.RowBased));
    }

    @Test
    public void testComputeFacets() {
        List<FacetState> states = SUT.getFacetStates();
        Assert.assertEquals(states, Arrays.asList(
                new FacetStateStub(1, 1, filterA),
                new FacetStateStub(2, 0, noFilter)));
    }

    @Test
    public void testGetMachingRows() {
        List<Tuple2<Long, Row>> rows = SUT.getMatchingRows().getGrid().collect();
        Assert.assertEquals(rows, Collections.singletonList(new Tuple2<Long, Row>(0L,
                new Row(Arrays.asList(new Cell("a", null), new Cell("b", null))))));
    }

    @Test
    public void testGetMismachingRows() {
        List<Tuple2<Long, Row>> rows = SUT.getMismatchingRows().getGrid().collect();
        Assert.assertEquals(rows, Collections.singletonList(new Tuple2<Long, Row>(1L,
                new Row(Arrays.asList(new Cell("c", null), new Cell("d", null))))));
    }

}
