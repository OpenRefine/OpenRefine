
package org.openrefine.model.changes;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.openrefine.browsing.Engine;
import org.openrefine.history.History;
import org.openrefine.model.Grid;
import org.openrefine.process.ProcessManager;
import org.openrefine.process.ProgressReporter;
import org.openrefine.process.ProgressingFuture;

/**
 * Stores and retrieves {@link ChangeData} objects keyed by a pair: - the id of the change it belongs to - a string id
 * for which part of the change it represents (such that changes can potentially register multiple change data)
 * <p>
 * A serializer is provided for both methods if they want to store the change data physically somewhere.
 */
public interface ChangeDataStore {

    ProcessManager getProcessManager();

    /**
     * Stores a {@link ChangeData}, which might imply explicitly computing all its values (if the store persists its
     * changes).
     *
     * @param data
     *            the data to store
     * @param changeDataId
     *            the id of the change data to store
     * @param serializer
     *            to serialize the data to a file, for instance
     * @param progressReporter
     *            reports the progress of the change data computation and serialization
     * @throws IOException
     *             if serialization failed
     */
    public <T> void store(
            ChangeData<T> data,
            ChangeDataId changeDataId,
            ChangeDataSerializer<T> serializer,
            Optional<ProgressReporter> progressReporter)
            throws IOException;

    /**
     * Asynchronously stores a {@link ChangeData}, which might imply explicitly computing all its values (if the store
     * persists its changes).
     *
     * @param data
     *            the data to store
     * @param changeDataId
     *            the id of the change data to store
     * @param serializer
     *            to serialize the data to a file, for instance
     */
    public <T> ProgressingFuture<Void> storeAsync(
            ChangeData<T> data,
            ChangeDataId changeDataId,
            ChangeDataSerializer<T> serializer);

    /**
     * Loads back a {@link ChangeData} that has been persisted before.
     *
     * @param changeDataId
     *            the id of the change data to retrieve
     * @param serializer
     *            the deserializer to read it back from a file
     */
    public <T> ChangeData<T> retrieve(
            ChangeDataId changeDataId,
            ChangeDataSerializer<T> serializer)
            throws IOException;

    /**
     * Retrieves a change data if it exists already, and if it does not (or is incomplete), start a process to finish
     * computing it.
     *
     * @param changeDataId
     *            the id of the change data to retrieve
     * @param serializer
     *            the serializer to read it back from a file and to write its completion if it is incomplete
     * @param baseGrid
     *            the grid based on which the change data will be computed
     * @param completionProcess
     *            a function taking the existing state of the change data and returning the complete version
     * @param description
     *            a description of the completion process, to be reported to the user
     * @param history
     *            the history of the project
     * @param requiredStepIndex
     *            the index of the step in the history from which this change data should be computed
     * @param engineMode
     *            whether this change data should be computed row- or record-wise
     */
    public <T> ChangeData<T> retrieveOrCompute(
            ChangeDataId changeDataId,
            ChangeDataSerializer<T> serializer,
            Grid baseGrid,
            Function<Optional<ChangeData<T>>, ChangeData<T>> completionProcess,
            String description,
            History history,
            int requiredStepIndex,
            Engine.Mode engineMode)
            throws IOException;

    /**
     * Returns true when a change data object is being fetched by a process and should therefore be refreshed as the
     * process may have refreshed the contents on disk.
     */
    public boolean needsRefreshing(long historyEntryId);

    /**
     * Retrieve the list of change data ids associated with a given history entry.
     */
    public List<ChangeDataId> getChangeDataIds(long historyEntryId);

    /**
     * Discards all change data objects which belong to a given history entry id.
     */
    public void discardAll(long historyEntryId);

    /**
     * Stops all the processes and shuts down the thread pool.
     */
    public void dispose();

}
