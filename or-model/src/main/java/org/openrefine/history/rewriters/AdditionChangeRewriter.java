package org.openrefine.history.rewriters;

import java.util.List;
import java.util.Map;

import org.openrefine.history.Change;
import org.openrefine.history.dag.AdditionSlice;

/**
 * Change rewriter for changes which add new columns,
 * possibly based on other columns.
 * 
 * @author Antonin Delpeuch
 */
public interface AdditionChangeRewriter extends ChangeRewriter {
    
    /**
     * Rewrites the change to use different column names as inputs.
     * 
     * @param renames
     *     a map from old names used by the current change to new
     *     names used by the rewritten change
     * @param columnNames
     *     the new list of column names before this change is applied.
     *     This is useful to update the location of the added columns.
     * @return
     *     the rewritten change, which depends on the updated columns
     */
    public Change rewriteChange(Map<String, String> renames, List<String> columnNames);
    
    @Override
    public AdditionSlice getDagSlice();
}
