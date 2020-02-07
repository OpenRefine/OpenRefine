
package org.openrefine.model.changes;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.history.Change;
import org.openrefine.model.GridState;

/**
 * Change which respects any facets applied to the project.
 * 
 * @author Antonin Delpeuch
 */
public abstract class EngineDependentChange implements Change {

    protected final EngineConfig _engineConfig;

    public EngineDependentChange(EngineConfig engineConfig) {
        _engineConfig = engineConfig;
    }

    @Override
    public final GridState apply(GridState projectState) {
        // Run the operation on the subset of the grid that matches
        Engine engine = new Engine(projectState, _engineConfig);
        GridState filteredResult = applyToFilteredState(engine.getMatchingRows());

        // Merge it with the mismatching rows
        if (!filteredResult.getColumnModel().equals(projectState.getColumnModel())) {
            throw new IllegalStateException("Padding with new empty columns on rows that are filtered out is unsupported");
        }
        GridState joined = filteredResult.union(engine.getMismatchingRows());
        return joined;
    }

    /**
     * Runs the operation on the subset of the project selected by the facets. The result of this method is then merged
     * by {@link apply} with the rest of the grid.
     *
     * @param filteredState
     *            the subset of the grid selected by the facets
     * @return the result of the operation on the selected rows
     */
    public abstract GridState applyToFilteredState(GridState filteredState);

}
