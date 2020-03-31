package org.openrefine.history.rewriters;

import org.openrefine.history.Change;
import org.openrefine.history.dag.DagSlice;

/**
 * An object associated with a change,
 * which can be used to translate the change to
 * a new context, such as one where the column dependencies
 * have been renamed or reordered.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface ChangeRewriter {
    
    /**
     * Get the original change that this rewriter works on.
     */
    public Change getChange();
    
    /**
     * Get the original DAG slice that the change conforms to.
     */
    public DagSlice getDagSlice();
}
