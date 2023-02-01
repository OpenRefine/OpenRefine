
package org.openrefine.model.changes;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

/**
 * Stores information that changes can rely on when modifying the project grid.
 * <p>
 * If we add multi-user support, we could for instance expose the user who committed the change, if we want this to let
 * this information influence the change.
 * 
 *
 */
public interface ChangeContext {

    /**
     * Creates a change context for a given history entry id and a project id.
     */
    static ChangeContext create(long historyEntryId, long projectId, ChangeDataStore dataStore, String description) {
        return new ChangeContextImpl(historyEntryId, projectId, dataStore, description);
    }

    long getHistoryEntryId();

    long getProjectId();

    String getChangeDescription();

    /**
     * Retrieves a {@link ChangeData} from the underlying {@link ChangeDataStore}. It must have been registered in the
     * store beforehand.
     */
    <T> ChangeData<T> getChangeData(String dataId, ChangeDataSerializer<T> serializer,
            Function<Optional<ChangeData<T>>, ChangeData<T>> completionProcess) throws IOException;

}
