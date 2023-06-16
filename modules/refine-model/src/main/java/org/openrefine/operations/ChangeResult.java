
package org.openrefine.operations;

import java.util.Collections;
import java.util.List;

import org.openrefine.browsing.facets.FacetConfig;
import org.openrefine.history.GridPreservation;
import org.openrefine.history.dag.DagSlice;
import org.openrefine.model.Grid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Bundles up various pieces of information after the application of an operation. Those are:
 * <ul>
 * <li>the new grid after applying the change</li>
 * <li>whether the rows or records of the original grid were preserved</li>
 * <li>a representation of the dependencies of this change</li>
 * <li>a list of facets that should be opened after this operation was run</li>
 * </ul>
 */
public class ChangeResult {

    protected final Grid grid;
    protected final DagSlice dagSlice;
    protected final List<FacetConfig> createdFacets;
    protected final GridPreservation gridPreservation;

    public ChangeResult(Grid grid, GridPreservation gridPreservation, DagSlice dagSlice) {
        this.grid = grid;
        this.gridPreservation = gridPreservation;
        this.createdFacets = Collections.emptyList();
        this.dagSlice = dagSlice;
    }

    public ChangeResult(Grid grid, GridPreservation gridPreservation, List<FacetConfig> createdFacets, DagSlice dagSlice) {
        this.grid = grid;
        this.gridPreservation = gridPreservation;
        this.createdFacets = createdFacets;
        this.dagSlice = dagSlice;
    }

    @JsonIgnore // because grids should not be serialized to JSON as a whole
    public Grid getGrid() {
        return grid;
    }

    @JsonIgnore // because already available from the HistoryEntry
    public GridPreservation getGridPreservation() {
        return gridPreservation;
    }

    /**
     * The facets that are suggested to be created after this change is applied.
     */
    @JsonProperty("createdFacets")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<FacetConfig> getCreatedFacets() {
        return createdFacets;
    }

    @JsonIgnore
    public DagSlice getDagSlice() {
        return dagSlice;
    }

}
