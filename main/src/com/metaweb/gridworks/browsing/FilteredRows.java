package com.metaweb.gridworks.browsing;

import com.metaweb.gridworks.model.Project;

/**
 * Interface for anything that can decide which rows match and which rows don't match
 * based on some particular criteria.
 */
public interface FilteredRows {
    /**
     * Go through the rows of the given project, determine which match and which don't,
     * and call visitor.visit() on those that match, and possibly their context and
     * dependent rows.
     * 
     * @param project
     * @param visitor
     */
    public void accept(Project project, RowVisitor visitor);
}
