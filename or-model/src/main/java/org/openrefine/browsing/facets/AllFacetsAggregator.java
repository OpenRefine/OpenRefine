package org.openrefine.browsing.facets;

import java.util.ArrayList;
import java.util.List;

import org.openrefine.model.Record;
import org.openrefine.model.RecordFilter;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Internal aggregator to compute the state of all facets
 * in one pass over the grid.
 * 
 * We use {@link com.google.common.collect.ImmutableList} to ensure immutability and serializability of the states.
 * 
 * @author Antonin Delpeuch
 *
 */
public class AllFacetsAggregator implements RowAggregator<AllFacetsState>, RecordAggregator<AllFacetsState> {
    
    private static final long serialVersionUID = -8277361327137906882L;
    
    protected final List<FacetAggregator<?>> _facetAggregators;
    protected final List<RowFilter>          _rowFilters;
    protected final List<RecordFilter>       _recordFilters;
    
    public AllFacetsAggregator(List<FacetAggregator<?>> facetAggregators) {
        _facetAggregators = facetAggregators;
        _rowFilters = new ArrayList<>(facetAggregators.size());
        _recordFilters = new ArrayList<>(facetAggregators.size());
        for(FacetAggregator<?> aggregator : facetAggregators) {
            _rowFilters.add(aggregator == null ? null : aggregator.getRowFilter());
            _recordFilters.add(aggregator == null ? null : aggregator.getRecordFilter()); 
        }
    }
    
    @Override
    public AllFacetsState withRow(AllFacetsState states, long rowId, Row row) {
        if (states.size() != _facetAggregators.size()) {
            throw new IllegalArgumentException("Incompatible list of facet states and facet aggregators");
        }
        // Compute whether each facet matches the row
        boolean[] matching = new boolean[_facetAggregators.size()];
        int numberOfMismatches = 0;
        for(int i = 0; i != _facetAggregators.size(); i++) {
            RowFilter filter = _rowFilters.get(i);
            matching[i] = filter == null || filter.filterRow(rowId, row);
            if (!matching[i]) {
                numberOfMismatches++;
            }
        }
        
        // Compute the new list of facet states
        boolean allMatching = numberOfMismatches == 0;
        Builder<FacetState> newStates = ImmutableList.<FacetState>builder();
        for(int i = 0; i != states.size(); i++) {
            // Rows are only seen by facets if they are selected by all other facets
            newStates.add(incrementHelper(_facetAggregators.get(i), states.get(i), rowId, row, allMatching || (numberOfMismatches == 1 && !matching[i])));
        }
        return new AllFacetsState(newStates.build(), states.getAggregatedCount()+1, states.getFilteredCount() + (numberOfMismatches == 0 ? 1 : 0));
    }
    
    @Override
    public AllFacetsState withRecord(AllFacetsState states, Record record) {
        if (states.size() != _facetAggregators.size()) {
            throw new IllegalArgumentException("Incompatible list of facet states and facet aggregators");
        }
        // Compute which whether each facet matches the record
        boolean[] matching = new boolean[_facetAggregators.size()];
        int numberOfMismatches = 0;
        for(int i = 0; i != _facetAggregators.size(); i++) {
            RecordFilter filter = _recordFilters.get(i);
            matching[i] = filter == null || filter.filterRecord(record);
            if (!matching[i]) {
                numberOfMismatches++;
            }
            // No need to keep evaluating facets if we already found
            // two mismatching facets: this row will not count towards any statistics.
            if(numberOfMismatches > 1) {
                return states;
            }
        }
        
        // Compute the new list of facet states
        boolean allMatching = numberOfMismatches == 0;
        Builder<FacetState> newStates = ImmutableList.<FacetState>builder();
        List<Row> rows = record.getRows();
        for(int i = 0; i != states.size(); i++) {
            // Rows are only seen by facets if they are selected by all other facets
            if(allMatching || !matching[i]) {
                FacetState currentState = states.get(i);
                for(int j = 0; j != rows.size(); j++) {
                    currentState = incrementHelper(_facetAggregators.get(i), currentState, record.getStartRowId()+j, rows.get(j), true);
                }
                newStates.add(currentState);
            } else {
                newStates.add(states.get(i));
            }
        }
        return new AllFacetsState(newStates.build(), states.getAggregatedCount()+1, states.getFilteredCount() + (numberOfMismatches == 0 ? 1 : 0));
    }
    
    @Override
    public AllFacetsState sum(AllFacetsState first, AllFacetsState second) {
        if(first.size() != second.size()) {
            throw new IllegalArgumentException("Attempting to merge incompatible AllFacetStates");
        }
        Builder<FacetState> newStates = ImmutableList.<FacetState>builder();
        for(int i = 0; i != first.size(); i++) {
            newStates.add(sumHelper(_facetAggregators.get(i), first.get(i), second.get(i)));
        }
        return new AllFacetsState(
                newStates.build(),
                first.getAggregatedCount() + second.getAggregatedCount(),
                first.getFilteredCount() + second.getFilteredCount());
    }
    
    @SuppressWarnings("unchecked")
    private static <T extends FacetState> T incrementHelper(FacetAggregator<T> aggregator, FacetState state, long rowId, Row row, boolean insideView) {
        if (aggregator == null) {
            return (T) state;
        } else if (insideView) {
            return aggregator.withRow((T) state, rowId, row);
        } else {
            return aggregator.withRowOutsideView((T) state, rowId, row);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static <T extends FacetState> T sumHelper(FacetAggregator<T> aggregator, FacetState first, FacetState second) {
        if (aggregator == null) {
            return (T) first;
        } else {
            return aggregator.sum((T)first, (T)second);
        }
    }
}
