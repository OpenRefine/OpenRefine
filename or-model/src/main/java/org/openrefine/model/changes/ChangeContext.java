
package org.openrefine.model.changes;

/**
 * Stores information that changes can rely on when modifying the project grid.
 * 
 * If we add multi-user support, we could for instance expose the user who commited the change, if we want this to let
 * this information influence the change.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface ChangeContext {

    /**
     * Creates a change context for a given history entry id.
     */
    public static ChangeContext create(long historyEntryId) {
        return new ChangeContextImpl(historyEntryId);
    }

    /**
     * The id of the change in the history.
     * 
     * @return
     */
    public long getHistoryEntryId();
}
