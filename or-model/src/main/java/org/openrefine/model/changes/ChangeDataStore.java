package org.openrefine.model.changes;

import java.io.IOException;
import java.io.Serializable;

/**
 * Stores and retrieves {@link ChangeData} objects keyed by a pair:
 * - the id of the change it belongs to
 * - a string id for which part of the change it represents (such that changes can potentially register
 *   multiple change data)
 *   
 * A serializer is provided for both methods if they want to store the change data physically somewhere.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface ChangeDataStore {
    
    /**
     * Stores a {@link ChangeData}, which might imply explicitly computing all its values
     * (if the store persists its changes).
     * 
     * @param <T>
     * @param data the data to store
     * @param historyEntryId the id of the change which generated this data
     * @param dataId the id of the dataset within the change
     * @param serializer to serialize the data to a file, for instance
     * @throws IOException if serialization failed
     */
    public <T extends Serializable> void store(
            ChangeData<T> data,
            long historyEntryId,
            String dataId,
            ChangeDataSerializer<T> serializer)
                    throws IOException;
    
    /**
     * Loads back a {@link ChangeData} that has been persisted before.
     * 
     * @param <T>
     * @param historyEntryId
     * @param dataId
     * @param serializer
     * @return
     * @throws IOException 
     */
    public <T extends Serializable> ChangeData<T> retrieve(
            long historyEntryId,
            String dataId,
            ChangeDataSerializer<T> serializer)
                    throws IOException;

}
