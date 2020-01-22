
package org.openrefine.browsing.facets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.Engine.Mode;
import org.openrefine.model.Row;

/**
 * Internal aggregator to compute the state of all facets in one pass over the grid.
 * 
 * @author Antonin Delpeuch
 *
 */
public class AllFacetsState implements Serializable {

    private static final long serialVersionUID = -8277361327137906882L;

    protected final List<FacetState> _facetStates;

    public AllFacetsState(List<FacetState> facetStates) {
        _facetStates = facetStates;
    }

    @JsonProperty("facets")
    public List<FacetState> getFacetStates() {
        return _facetStates;
    }

    @JsonProperty("engine-mode")
    Mode getEngineMode() {
        return Mode.RowBased;
    }

    public AllFacetsState increment(long rowId, Row row) {
        // Compute which whether each facet matches the row
        boolean[] matching = new boolean[_facetStates.size()];
        int numberOfMismatches = 0;
        for (int i = 0; i != _facetStates.size(); i++) {
            FacetState state = _facetStates.get(i);
            matching[i] = state.getRowFilter().filterRow(rowId, row);
            if (!matching[i]) {
                numberOfMismatches++;
            }
            // No need to keep evaluating facets if we already found
            // two mismatching facets: this row will not count towards any statistics.
            if (numberOfMismatches > 1) {
                return this;
            }
        }

        // Compute the new list of facet states
        List<FacetState> newStates = new ArrayList<>(_facetStates.size());
        for (int i = 0; i != _facetStates.size(); i++) {
            newStates.add(_facetStates.get(i).withRow(rowId, row));
        }
        return new AllFacetsState(newStates);
    }

    public AllFacetsState merge(AllFacetsState other) {
        List<FacetState> otherStates = other.getFacetStates();
        if (otherStates.size() != _facetStates.size()) {
            throw new IllegalArgumentException("Attempting to merge incompatible AllFacetStates");
        }
        List<FacetState> newStates = new ArrayList<>(_facetStates.size());
        for (int i = 0; i != _facetStates.size(); i++) {
            newStates.add(_facetStates.get(i).sum(otherStates.get(i)));
        }
        return new AllFacetsState(newStates);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AllFacetsState)) {
            return false;
        }
        AllFacetsState otherState = (AllFacetsState) other;
        return _facetStates.equals(otherState.getFacetStates());
    }

    @Override
    public String toString() {
        return String.join(", ",
                _facetStates.stream().map(s -> s.toString())
                        .collect(Collectors.toList()));
    }
}
