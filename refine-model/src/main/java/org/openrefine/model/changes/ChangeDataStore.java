
package org.openrefine.model.changes;

import java.io.IOException;
import java.util.Optional;

import org.openrefine.process.ProgressReporter;

/**
 * Stores and retrieves {@link ChangeData} objects keyed by a pair: - the id of the change it belongs to - a string id
 * for which part of the change it represents (such that changes can potentially register multiple change data)
 * <p>
 * A serializer is provided for both methods if they want to store the change data physically somewhere.
 * 
 *
 */
public interface ChangeDataStore {

    /**
     * Stores a {@link ChangeData}, which might imply explicitly computing all its values (if the store persists its
     * changes).
     *
     * @param data
     *            the data to store
     * @param historyEntryId
     *            the id of the change which generated this data
     * @param dataId
     *            the id of the dataset within the change
     * @param serializer
     *            to serialize the data to a file, for instance
     * @param progressReporter
     *            reports the progress of the change data computation and serialization
     * @throws IOException
     *             if serialization failed
     */
    public <T> void store(
            ChangeData<T> data,
            long historyEntryId,
            String dataId,
            ChangeDataSerializer<T> serializer,
            Optional<ProgressReporter> progressReporter)
            throws IOException;

    /**
     * Loads back a {@link ChangeData} that has been persisted before.
     *
     * @param historyEntryId
     *            the id of the change which generated this data
     * @param dataId
     *            the id of the dataset within the change
     * @param serializer
     *            the deserializer to read it back from a file
     */
    public <T> ChangeData<T> retrieve(
            long historyEntryId,
            String dataId,
            ChangeDataSerializer<T> serializer)
            throws IOException;

    /**
     * Discards all change data objects which belong to a given history entry id.
     */
    public void discardAll(long historyEntryId);

}
