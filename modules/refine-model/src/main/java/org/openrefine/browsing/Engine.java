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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import org.openrefine.browsing.columns.ColumnStats;
import org.openrefine.browsing.facets.AllFacetsAggregator;
import org.openrefine.browsing.facets.AllFacetsState;
import org.openrefine.browsing.facets.Facet;
import org.openrefine.browsing.facets.FacetResult;
import org.openrefine.browsing.facets.FacetState;
import org.openrefine.browsing.facets.RecordAggregator;
import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.model.Grid;
import org.openrefine.model.Grid.PartialAggregation;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Record;
import org.openrefine.model.RecordFilter;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.sorting.SortingConfig;
import org.openrefine.util.CloseableIterator;

/**
 * Faceted browsing engine. Given a {@link Grid} and facet configurations, it can be used to compute facet statistics
 * and obtain a filtered view of the grid according to the facets. <br>
 * It also computes datatype statistics for each column, serialized in the "columnStats" JSON field.
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

    protected final Grid _state;
    protected final List<Facet> _facets;
    protected final EngineConfig _config;
    protected PartialAggregation<AllFacetsState> _facetsState;

    static public String modeToString(Mode mode) {
        return mode == Mode.RowBased ? MODE_ROW_BASED : MODE_RECORD_BASED;
    }

    static public Mode stringToMode(String s) {
        return MODE_ROW_BASED.equals(s) ? Mode.RowBased : Mode.RecordBased;
    }

    public Engine(Grid state, EngineConfig config) {
        _state = state;
        _config = config;
        _facets = config.getFacetConfigs().stream()
                .map(fc -> fc.apply(state.getColumnModel(), state.getOverlayModels()))
                .collect(Collectors.toList());
        _facetsState = null;

    }

    @JsonProperty("engine-mode")
    public Mode getMode() {
        return _config.getMode();
    }

    @JsonIgnore
    public Grid getGrid() {
        return _state;
    }

    @JsonIgnore
    public EngineConfig getConfig() {
        return _config;
    }

    @JsonIgnore
    protected PartialAggregation<AllFacetsState> getFacetsState() {
        if (_facetsState == null) {
            if (_config.getAggregationLimit() == null) {
                AllFacetsState aggregated = _state.aggregateRows(allFacetsAggregator(), allFacetsInitialState());
                _facetsState = new PartialAggregation<AllFacetsState>(
                        aggregated,
                        aggregated.getAggregatedCount(),
                        false);
            } else {
                _facetsState = _state.aggregateRowsApprox(allFacetsAggregator(), allFacetsInitialState(), _config.getAggregationLimit());
            }
        }
        return _facetsState;
    }

    @JsonProperty("facets")
    public ImmutableList<FacetResult> getFacetResults() {
        AllFacetsState states = getFacetsState().getState();

        Builder<FacetResult> facetResults = ImmutableList.<FacetResult> builder();
        for (int i = 0; i != states.size(); i++) {
            facetResults.add(_facets.get(i).getFacetResult(states.get(i)));
        }
        return facetResults.build();
    }

    @JsonProperty("columnStats")
    public List<ColumnStats> getColumnStats() {
        return getFacetsState().getState().getColumnStats();
    }

    @JsonProperty("aggregatedCount")
    public long getAggregatedCount() {
        return getFacetsState().getProcessed();
    }

    @JsonProperty("filteredCount")
    public long getFilteredCount() {
        return getFacetsState().getState().getFilteredCount();
    }

    @JsonProperty("limitReached")
    public boolean limitReached() {
        return getFacetsState().limitReached();
    }

    /**
     * Iterates over the rows matched by the given filters. If the engine is in records mode, the rows corresponding to
     * the matching records are returned.
     * 
     * @param sortingConfig
     *            in which order to iterate over rows
     */
    @JsonIgnore
    public CloseableIterator<IndexedRow> getMatchingRows(SortingConfig sortingConfig) {
        Grid sorted = _state;
        if (!sortingConfig.getCriteria().isEmpty()) {
            // TODO refactor this so that we are not re-sorting the grid at every request, but cache it instead?
            if (Mode.RowBased.equals(getMode())) {
                sorted = _state.reorderRows(sortingConfig, false);
            } else {
                sorted = _state.reorderRecords(sortingConfig, false);
            }
        }
        if (Mode.RowBased.equals(getMode())) {
            return sorted.iterateRows(combinedRowFilters());
        } else {
            CloseableIterator<Record> recordsIterator = sorted.iterateRecords(combinedRecordFilters());
            return recordsIterator.flatMapCloseable(r -> CloseableIterator.wrapping(r.getIndexedRows().iterator()));
        }
    }

    /**
     * Iterates over the records matched by the given filters. If the engine is in records mode, the rows corresponding
     * to the matching records are returned.
     * 
     * @param sortingConfig
     *            in which order to iterate over records
     */
    @JsonIgnore
    public CloseableIterator<Record> getMatchingRecords(SortingConfig sortingConfig) {
        if (Mode.RowBased.equals(getMode())) {
            throw new IllegalStateException("Cannot iterate over records in rows mode");
        }
        // TODO refactor this so that we are not resorting the grid at each request, but cache it instead?
        return _state.reorderRecords(sortingConfig, false).iterateRecords(combinedRecordFilters());
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

    /**
     * Runs an aggregator only on the rows that are selected by facets.
     *
     * @param aggregator
     *            the aggregator to run on the selected rows
     * @param initialState
     *            the initial state of the aggregator (which should act as neutral element)
     */
    public <T extends Serializable> T aggregateFilteredRows(RowAggregator<T> aggregator, T initialState) {
        return _state.aggregateRows(restrictAggregator(aggregator, combinedRowFilters()), initialState);
    }

    /**
     * Runs an aggregator only on the records that are selected by facets.
     *
     * @param aggregator
     *            the aggregator to run on the selected records
     * @param initialState
     *            the initial state of the aggregator (which should act as neutral element)
     */
    public <T extends Serializable> T aggregateFilteredRecords(RecordAggregator<T> aggregator, T initialState) {
        return _state.aggregateRecords(restrictAggregator(aggregator, combinedRecordFilters()), initialState);
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
                .map(aggregator -> aggregator == null ? null : aggregator.getRowFilter())
                .filter(filter -> filter != null)
                .collect(Collectors.toList());
    }

    @JsonIgnore
    private AllFacetsState allFacetsInitialState() {
        ImmutableList<FacetState> facets = ImmutableList.copyOf(
                _facets
                        .stream().map(facet -> facet.getInitialFacetState())
                        .collect(Collectors.toList()));
        ImmutableList<ColumnStats> columnStats = ImmutableList
                .copyOf(Collections.nCopies(_state.getColumnModel().getColumns().size(), ColumnStats.ZERO));
        return new AllFacetsState(facets, columnStats, 0L, 0L);
    }

    @JsonIgnore
    private AllFacetsAggregator allFacetsAggregator() {
        return new AllFacetsAggregator(_facets
                .stream().map(facet -> facet.getAggregator())
                .collect(Collectors.toList()));
    }

    private static <T> RowAggregator<T> restrictAggregator(RowAggregator<T> aggregator, RowFilter filter) {
        return new RowAggregator<T>() {

            private static final long serialVersionUID = 8407224640500910094L;

            @Override
            public T sum(T first, T second) {
                return aggregator.sum(first, second);
            }

            @Override
            public T withRow(T state, long rowId, Row row) {
                if (filter.filterRow(rowId, row)) {
                    return aggregator.withRow(state, rowId, row);
                } else {
                    return state;
                }
            }

        };
    }

    private static <T> RecordAggregator<T> restrictAggregator(RecordAggregator<T> aggregator, RecordFilter filter) {
        return new RecordAggregator<T>() {

            private static final long serialVersionUID = 8407224640500910094L;

            @Override
            public T sum(T first, T second) {
                return aggregator.sum(first, second);
            }

            @Override
            public T withRecord(T state, Record record) {
                if (filter.filterRecord(record)) {
                    return aggregator.withRecord(state, record);
                } else {
                    return state;
                }
            }

        };
    }
}
