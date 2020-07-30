package org.openrefine.model.changes;

import java.io.IOException;
import java.io.Serializable;

/**
 * Stores information that changes can rely on when modifying the
 * project grid.
 * 
 * If we add multi-user support, we could for instance expose the user
 * who committed the change, if we want this to let this information 
 * influence the change.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface ChangeContext {
    
    /**
     * Creates a change context for a given history entry id.
     */
    public static ChangeContext create(long historyEntryId, ChangeDataStore dataStore) {
        return new ChangeContextImpl(historyEntryId, dataStore);
    }
    
    /**
     * The id of the change in the history.
     * @return
     */
    public long getHistoryEntryId();
    
    /**
     * Retrieves a {@link ChangeData} from the underlying
     * {@link ChangeDataStore}. It must have been registered
     * in the store before hand.
     */
    public <T extends Serializable> ChangeData<T> getChangeData(String dataId, ChangeDataSerializer<T> serializer) throws IOException;
}
