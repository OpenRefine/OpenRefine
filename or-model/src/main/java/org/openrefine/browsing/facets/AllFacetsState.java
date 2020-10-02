package org.openrefine.browsing.facets;

import com.google.common.collect.ImmutableList;

/**
 * State of aggregation of all facets, used to compute
 * the states of all facets in a single aggregation over the 
 * grid.
 * 
 * @author Antonin Delpeuch
 *
 */
public class AllFacetsState implements FacetState {

    private static final long serialVersionUID = -3089405630666554348L;
    private final ImmutableList<FacetState> _states;
    private final long                      _aggregatedCount;
    private final long                      _filteredCount;
    
    /**
     * Constructs a state for aggregation of all facets.
     * 
     * @param states
     * @param aggregatedCount
     * @param filteredCount
     */
    public AllFacetsState(
            ImmutableList<FacetState> states,
            long aggregatedCount,
            long filteredCount) {
        _states = states;
        _aggregatedCount = aggregatedCount;
        _filteredCount = filteredCount;
    }
    
    /**
     * Helper for facet access.
     * 
     * @param index position of the facet state to retrieve
     * @return
     */
    public FacetState get(int index) {
        return _states.get(index);
    }

    /**
     * Number of facet states stored in this state.
     */
    public int size() {
        return _states.size();
    }
    
    /**
     * List of all facet states
     */
    public ImmutableList<FacetState> getStates() {
        return _states;
    }
    
    /**
     * Number of rows or records seen by the aggregator.
     */
    public long getAggregatedCount() {
        return _aggregatedCount;
    }
    
    /**
     * Numbers of rows or records seen by the aggregator which matched
     * all facets
     */
    public long getFilteredCount() {
        return _filteredCount;
    }
    
    @Override
    public String toString() {
        return String.format("[AllFacetsState (after %d rows/records whose %d matched) %s]",
                _aggregatedCount, _filteredCount, _states.toString());
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AllFacetsState)) {
            return false;
        }
        AllFacetsState otherState = (AllFacetsState) other;
        return (_states.equals(otherState.getStates())
                && _aggregatedCount == otherState.getAggregatedCount()
                && _filteredCount == otherState.getFilteredCount());
    }
    
    @Override
    public int hashCode() {
        return _states.hashCode();
    }

}