
package org.openrefine.browsing.facets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.openrefine.browsing.RowFilter;
import org.openrefine.model.Row;

/**
 * Internal aggregator to compute the state of all facets in one pass over the grid.
 * 
 * @author Antonin Delpeuch
 *
 */
public class AllFacetsAggregator implements Serializable {

    private static final long serialVersionUID = -8277361327137906882L;

    protected final List<FacetAggregator<?>> _facetAggregators;
    protected final List<RowFilter> _rowFilters;

    public AllFacetsAggregator(List<FacetAggregator<?>> facetAggregators) {
        _facetAggregators = facetAggregators;
        _rowFilters = new ArrayList<>(facetAggregators.size());
        for (FacetAggregator<?> aggregator : facetAggregators) {
            _rowFilters.add(aggregator == null ? null : aggregator.getRowFilter());
        }
    }

    public List<FacetState> increment(List<FacetState> states, long rowId, Row row) {
        if (states.size() != _facetAggregators.size()) {
            throw new IllegalArgumentException("Incompatible list of facet states and facet aggregators");
        }
        // Compute which whether each facet matches the row
        boolean[] matching = new boolean[_facetAggregators.size()];
        int numberOfMismatches = 0;
        for (int i = 0; i != _facetAggregators.size(); i++) {
            RowFilter filter = _rowFilters.get(i);
            matching[i] = filter == null || filter.filterRow(rowId, row);
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
        List<FacetState> newStates = new ArrayList<>(states.size());
        for (int i = 0; i != states.size(); i++) {
            // Rows are only seen by facets if they are selected by all other facets
            if (allMatching || !matching[i]) {
                newStates.add(incrementHelper(_facetAggregators.get(i), states.get(i), rowId, row));
            } else {
                newStates.add(states.get(i));
            }
        }
        return newStates;
    }

    public List<FacetState> sum(List<FacetState> first, List<FacetState> second) {
        if (first.size() != second.size()) {
            throw new IllegalArgumentException("Attempting to merge incompatible AllFacetStates");
        }
        List<FacetState> newStates = new ArrayList<>(first.size());
        for (int i = 0; i != first.size(); i++) {
            newStates.add(sumHelper(_facetAggregators.get(i), first.get(i), second.get(i)));
        }
        return newStates;
    }

    @SuppressWarnings("unchecked")
    private static <T extends FacetState> T incrementHelper(FacetAggregator<T> aggregator, FacetState state, long rowId, Row row) {
        if (aggregator == null) {
            return (T) state;
        } else {
            return aggregator.withRow((T) state, rowId, row);
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
