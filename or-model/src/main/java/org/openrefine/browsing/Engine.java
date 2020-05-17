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

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.facets.AllFacetsAggregator;
import org.openrefine.browsing.facets.Facet;
import org.openrefine.browsing.facets.FacetResult;
import org.openrefine.browsing.facets.FacetState;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Record;

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

    @JsonProperty("facets")
    public List<FacetResult> getFacetResults() {
        return _state.computeRowFacets(_facets);
    }

    @JsonIgnore
    public Iterable<IndexedRow> getMatchingRows() {
        if (Mode.RowBased.equals(getMode())) {
            return _state.iterateRows(combinedRowFilters());
        } else {
            Iterable<Record> records = _state.iterateRecords(combinedRecordFilters());
            return new Iterable<IndexedRow>() {

                @Override
                public Iterator<IndexedRow> iterator() {
                    Iterator<Record> recordIterator = records.iterator();
                    return new Iterator<IndexedRow>() {

                        private Iterator<IndexedRow> currentRecord = null;

                        @Override
                        public boolean hasNext() {
                            return (currentRecord != null && currentRecord.hasNext()) || recordIterator.hasNext();
                        }

                        @Override
                        public IndexedRow next() {
                            if (currentRecord == null || !currentRecord.hasNext()) {
                                currentRecord = recordIterator.next().getIndexedRows().iterator();
                            }
                            return currentRecord.next();
                        }

                    };
                }

            };
        }
    }

    /**
     * @return a row filter obtained from all applied facets
     */
    @JsonIgnore
    public RowFilter combinedRowFilters() {
        return RowFilter.conjunction(facetRowFilters());
    }

    /**
     * @return a record filter obtained from all applied facets
     */
    @JsonIgnore
    public RecordFilter combinedRecordFilters() {
        return RecordFilter.conjunction(facetRecordFilters());
    }

    @JsonIgnore
    private List<RowFilter> facetRowFilters() {
        return _facets.stream()
                .map(facet -> facet.getAggregator())
                .map(aggregator -> aggregator == null ? null : aggregator.getRowFilter())
                .filter(filter -> filter != null)
                .collect(Collectors.toList());
    }

    @JsonIgnore
    private List<RecordFilter> facetRecordFilters() {
        return _facets.stream()
                .map(facet -> facet.getAggregator())
                .map(aggregator -> aggregator == null ? null : aggregator.getRecordFilter())
                .filter(filter -> filter != null)
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
}
