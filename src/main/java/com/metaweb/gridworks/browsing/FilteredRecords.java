package com.metaweb.gridworks.browsing;

import com.metaweb.gridworks.model.Project;

/**
 * Interface for anything that can decide which records match and which don't
 * based on some particular criteria.
 */
public interface FilteredRecords {
    /**
     * Go through the records of the given project, determine which match and which don't,
     * and call visitor.visit() on those that match
     * 
     * @param project
     * @param visitor
     */
    public void accept(Project project, RecordVisitor visitor);
}
