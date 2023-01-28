
package org.openrefine.browsing.facets;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import org.openrefine.browsing.columns.ColumnStats;
import org.openrefine.model.*;
import org.openrefine.model.Record;

/**
 * Internal aggregator to compute the state of all facets in one pass over the grid. <br>
 * We use {@link com.google.common.collect.ImmutableList} to ensure immutability and serializability of the states.
 * 
 *
 */
public class AllFacetsAggregator extends RowInRecordAggregator<AllFacetsState> {

    private static final long serialVersionUID = -8277361327137906882L;

    protected final List<FacetAggregator<?>> _facetAggregators;
    protected final List<RowInRecordFilter> _rowFilters;

    public AllFacetsAggregator(List<FacetAggregator<?>> facetAggregators) {
        _facetAggregators = facetAggregators;
        _rowFilters = new ArrayList<>(facetAggregators.size());
        for (FacetAggregator<?> aggregator : facetAggregators) {
            _rowFilters.add(aggregator == null ? null : aggregator.getRowFilter());
        }
    }

    @Override
    public AllFacetsState withRow(AllFacetsState states, long rowId, Row row, Record record) {
        if (states.size() != _facetAggregators.size()) {
            throw new IllegalArgumentException("Incompatible list of facet states and facet aggregators");
        }
        // Compute whether each facet matches the row
        boolean[] matching = new boolean[_facetAggregators.size()];
        int numberOfMismatches = 0;
        for (int i = 0; i != _facetAggregators.size(); i++) {
            RowFilter filter = _rowFilters.get(i);
            matching[i] = filter == null || filter.filterRow(rowId, row);
            if (!matching[i]) {
                numberOfMismatches++;
            }
        }

        // Compute the new list of facet states
        boolean allMatching = numberOfMismatches == 0;
        Builder<FacetState> newStates = ImmutableList.<FacetState> builder();
        for (int i = 0; i != states.size(); i++) {
            // Rows are only seen by facets if they are selected by all other facets
            newStates.add(incrementHelper(_facetAggregators.get(i), states.get(i), rowId, row, record,
                    allMatching || (numberOfMismatches == 1 && !matching[i])));
        }

        // Compute the new list of datatype statistics
        ImmutableList<ColumnStats> newColumnStats = updateColumnStats(states.getColumnStats(), row);

        return new AllFacetsState(newStates.build(), newColumnStats, states.getAggregatedCount() + 1,
                states.getFilteredCount() + (numberOfMismatches == 0 ? 1 : 0));
    }

    @Override
    public AllFacetsState withRecord(AllFacetsState states, Record record) {
        if (states.size() != _facetAggregators.size()) {
            throw new IllegalArgumentException("Incompatible list of facet states and facet aggregators");
        }
        // Compute which whether each facet matches the record
        boolean[] matching = new boolean[_facetAggregators.size()];
        int numberOfMismatches = 0;
        for (int i = 0; i != _facetAggregators.size(); i++) {
            RecordFilter filter = _rowFilters.get(i);
            matching[i] = filter == null || filter.filterRecord(record);
            if (!matching[i]) {
                numberOfMismatches++;
            }
            // No need to keep evaluating facets if we already found
            // two mismatching facets: this row will not count towards any statistics.
            if (numberOfMismatches > 1) {
                return states;
            }
        }

        // Compute the new list of facet states
        boolean allMatching = numberOfMismatches == 0;
        Builder<FacetState> newStates = ImmutableList.<FacetState> builder();
        List<Row> rows = record.getRows();
        for (int i = 0; i != states.size(); i++) {
            // Rows are only seen by facets if they are selected by all other facets
            if (allMatching || !matching[i]) {
                FacetState currentState = states.get(i);
                for (int j = 0; j != rows.size(); j++) {
                    currentState = incrementHelper(_facetAggregators.get(i), currentState, record.getStartRowId() + j, rows.get(j), record,
                            true);
                }
                newStates.add(currentState);
            } else {
                newStates.add(states.get(i));
            }
        }

        // Compute the new column statistics
        ImmutableList<ColumnStats> newColumnStats = states.getColumnStats();
        for (Row row : record.getRows()) {
            newColumnStats = updateColumnStats(newColumnStats, row);
        }

        return new AllFacetsState(newStates.build(), newColumnStats, states.getAggregatedCount() + 1,
                states.getFilteredCount() + (numberOfMismatches == 0 ? 1 : 0));
    }

    @Override
    public AllFacetsState sum(AllFacetsState first, AllFacetsState second) {
        if (first.size() != second.size()) {
            throw new IllegalArgumentException("Attempting to merge incompatible AllFacetStates");
        }
        Builder<FacetState> newStates = ImmutableList.<FacetState> builder();
        for (int i = 0; i != first.size(); i++) {
            newStates.add(sumHelper(_facetAggregators.get(i), first.get(i), second.get(i)));
        }
        return new AllFacetsState(
                newStates.build(),
                sumColumnStats(first.getColumnStats(), second.getColumnStats()),
                first.getAggregatedCount() + second.getAggregatedCount(),
                first.getFilteredCount() + second.getFilteredCount());
    }

    protected ImmutableList<ColumnStats> updateColumnStats(List<ColumnStats> statistics, Row row) {
        List<ColumnStats> newColumnStats = new ArrayList<>(statistics.size());
        if (row.getCells().size() != statistics.size()) {
            throw new IllegalStateException("Incompatible list of column statistics and row size");
        }
        List<Cell> cells = row.getCells();
        for (int i = 0; i != cells.size(); i++) {
            newColumnStats.add(statistics.get(i).withCell(cells.get(i)));
        }
        return ImmutableList.copyOf(newColumnStats);
    }

    protected ImmutableList<ColumnStats> sumColumnStats(List<ColumnStats> first, List<ColumnStats> second) {
        if (first.size() != second.size()) {
            throw new IllegalStateException("Incompatible list of column statistics to sum together");
        }
        List<ColumnStats> columnStats = new ArrayList<>(first.size());
        for (int i = 0; i != first.size(); i++) {
            columnStats.add(first.get(i).sum(second.get(i)));
        }
        return ImmutableList.copyOf(columnStats);
    }

    @SuppressWarnings("unchecked")
    private static <T extends FacetState> T incrementHelper(FacetAggregator<T> aggregator, FacetState state, long rowId, Row row,
            Record record, boolean insideView) {
        if (aggregator == null) {
            return (T) state;
        } else if (insideView) {
            return aggregator.withRow((T) state, rowId, row);
        } else {
            return aggregator.withRowOutsideView((T) state, rowId, row, record);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends FacetState> T sumHelper(FacetAggregator<T> aggregator, FacetState first, FacetState second) {
        if (aggregator == null) {
            return (T) first;
        } else {
            return aggregator.sum((T) first, (T) second);
        }
    }
}
