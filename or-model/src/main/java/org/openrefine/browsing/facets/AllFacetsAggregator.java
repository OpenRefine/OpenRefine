
package org.openrefine.browsing.facets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

    public AllFacetsAggregator(List<FacetAggregator<?>> facetAggregators) {
        _facetAggregators = facetAggregators;
    }

    public List<FacetState> increment(List<FacetState> states, long rowId, Row row) {
        if (states.size() != _facetAggregators.size()) {
            throw new IllegalArgumentException("Incompatible list of facet states and facet aggregators");
        }
        // Compute which whether each facet matches the row
        boolean[] matching = new boolean[_facetAggregators.size()];
        int numberOfMismatches = 0;
        for (int i = 0; i != _facetAggregators.size(); i++) {
            FacetAggregator<?> aggregator = _facetAggregators.get(i);
            matching[i] = aggregator.getRowFilter().filterRow(rowId, row);
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
        List<FacetState> newStates = new ArrayList<>(states.size());
        for (int i = 0; i != states.size(); i++) {
            newStates.add(incrementHelper(_facetAggregators.get(i), states.get(i), rowId, row));
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
        return aggregator.withRow((T) state, rowId, row);
    }

    @SuppressWarnings("unchecked")
    private static <T extends FacetState> T sumHelper(FacetAggregator<T> aggregator, FacetState first, FacetState second) {
        return aggregator.sum((T) first, (T) second);
    }
}
