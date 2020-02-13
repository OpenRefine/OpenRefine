/**

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

package org.openrefine.browsing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import scala.Tuple2;

import org.openrefine.browsing.facets.AllFacetsAggregator;
import org.openrefine.browsing.facets.Facet;
import org.openrefine.browsing.facets.FacetResult;
import org.openrefine.browsing.facets.FacetState;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;

/**
 * Faceted browsing engine. Given a GridState and facet configurations, it can be used to compute facet statistics and
 * obtain a filtered view of the grid according to the facets.
 */
public class Engine {

    static public enum Mode {
        @JsonProperty(MODE_ROW_BASED)
        RowBased, @JsonProperty(MODE_RECORD_BASED)
        RecordBased
    }

    public final static String INCLUDE_DEPENDENT = "includeDependent";
    public final static String MODE = "mode";
    public final static String MODE_ROW_BASED = "row-based";
    public final static String MODE_RECORD_BASED = "record-based";

    protected final GridState _state;
    protected final List<Facet> _facets;
    protected final EngineConfig _config;

    static public String modeToString(Mode mode) {
        return mode == Mode.RowBased ? MODE_ROW_BASED : MODE_RECORD_BASED;
    }

    static public Mode stringToMode(String s) {
        return MODE_ROW_BASED.equals(s) ? Mode.RowBased : Mode.RecordBased;
    }

    public Engine(GridState state, EngineConfig config) {
        _state = state;
        _config = config;
        _facets = config.getFacetConfigs().stream()
                .map(fc -> fc.apply(state.getColumnModel()))
                .collect(Collectors.toList());

    }

    @JsonProperty("engine-mode")
    public Mode getMode() {
        return _config.getMode();
    }

    @JsonIgnore
    public GridState getGridState() {
        return _state;
    }

    @JsonIgnore
    public EngineConfig getConfig() {
        return _config;
    }

    /**
     * Computes a grid state which only contains the rows matching the current facets.
     * 
     * @return
     */
    @JsonIgnore
    public GridState getMatchingRows() {
        Function<Tuple2<Long, Row>, Boolean> f = rowFilterConjuction(facetRowFilters());
        JavaPairRDD<Long, Row> matchingRows = _state.getGrid().filter(f);
        return new GridState(_state.getColumnModel(), matchingRows, _state.getOverlayModels());
    }

    /**
     * Computes a grid state which only contains all the rows not matched by the current facets
     * 
     * @return
     */
    @JsonIgnore
    public GridState getMismatchingRows() {
        Function<Tuple2<Long, Row>, Boolean> f = negatedRowFilterConjuction(facetRowFilters());
        JavaPairRDD<Long, Row> matchingRows = _state.getGrid().filter(f);
        return new GridState(_state.getColumnModel(), matchingRows, _state.getOverlayModels());
    }

    @JsonProperty("facets")
    public List<FacetResult> getFacetResults() {
        List<FacetState> states = getFacetStates();
        List<FacetResult> facetResults = new ArrayList<>();
        for (int i = 0; i != states.size(); i++) {
            facetResults.add(_facets.get(i).getFacetResult(states.get(i)));
        }
        return facetResults;
    }

    /**
     * Computes the facet states of the configured facets on the current grid
     */
    @JsonIgnore
    public List<FacetState> getFacetStates() {
        if (_config.getMode().equals(Mode.RowBased)) {
            AllFacetsAggregator aggregator = new AllFacetsAggregator(
                    _facets.stream().map(f -> f.getAggregator()).collect(Collectors.toList()));
            return _state.getGrid().aggregate(allFacetsInitialState(), rowSeqOp(aggregator), facetCombineOp(aggregator));
        } else {
            throw new IllegalStateException("not implemented");
        }
    }

    @JsonIgnore
    private List<RowFilter> facetRowFilters() {
        return _facets.stream()
                .map(facet -> facet.getAggregator())
                .map(aggregator -> aggregator == null ? null : aggregator.getRowFilter())
                .collect(Collectors.toList());
    }

    @JsonIgnore
    private List<FacetState> allFacetsInitialState() {
        return _facets
                .stream().map(facet -> facet.getInitialFacetState())
                .collect(Collectors.toList());
    }

    @JsonIgnore
    private AllFacetsAggregator allFacetsAggregator() {
        return new AllFacetsAggregator(_facets
                .stream().map(facet -> facet.getAggregator())
                .collect(Collectors.toList()));
    }

    /**
     * Functions used to compute facet statistics
     */

    private static Function2<List<FacetState>, Tuple2<Long, Row>, List<FacetState>> rowSeqOp(AllFacetsAggregator aggregator) {
        return new Function2<List<FacetState>, Tuple2<Long, Row>, List<FacetState>>() {

            private static final long serialVersionUID = 1L;

            @Override
            public List<FacetState> call(List<FacetState> states, Tuple2<Long, Row> rowTuple) throws Exception {
                return aggregator.increment(states, rowTuple._1, rowTuple._2);
            }
        };
    }

    private static Function2<List<FacetState>, List<FacetState>, List<FacetState>> facetCombineOp(AllFacetsAggregator aggregator) {
        return new Function2<List<FacetState>, List<FacetState>, List<FacetState>>() {

            private static final long serialVersionUID = 1L;

            @Override
            public List<FacetState> call(List<FacetState> statesA, List<FacetState> statesB) throws Exception {
                return aggregator.sum(statesA, statesB);
            }
        };
    }

    private static Function<Tuple2<Long, Row>, Boolean> rowFilterConjuction(List<RowFilter> rowFilters) {
        return new Function<Tuple2<Long, Row>, Boolean>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Boolean call(Tuple2<Long, Row> v1) throws Exception {
                return rowFilters.stream().allMatch(f -> f == null || f.filterRow(v1._1, v1._2));
            }
        };
    }

    private static Function<Tuple2<Long, Row>, Boolean> negatedRowFilterConjuction(List<RowFilter> rowFilters) {
        return new Function<Tuple2<Long, Row>, Boolean>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Boolean call(Tuple2<Long, Row> v1) throws Exception {
                return !rowFilters.stream().allMatch(f -> f.filterRow(v1._1, v1._2));
            }
        };
    }
}
