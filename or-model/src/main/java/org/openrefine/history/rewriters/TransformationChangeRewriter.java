package org.openrefine.history.rewriters;

import java.util.Map;

import org.openrefine.history.Change;
import org.openrefine.history.dag.TransformationSlice;

/**
 * Change rewriter for changes which transform a single column,
 * possibly based on other columns.
 * 
 * @author Antonin Delpeuch
 */
public interface TransformationChangeRewriter extends ChangeRewriter {
    
    /**
     * Rewrites the change to use different column names as inputs.
     * 
     * @param renames
     *     a map from old names used by the current change to new
     *     names used by the rewritten change
     * @return
     *     the rewritten change, which depends on the updated columns
     */
    public Change rewriteChange(Map<String, String> renames);
    
    @Override
    public TransformationSlice getDagSlice();
}
