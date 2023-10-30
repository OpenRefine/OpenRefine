
package org.openrefine.model.changes;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import org.openrefine.browsing.Engine;
import org.openrefine.history.History;
import org.openrefine.model.ColumnId;
import org.openrefine.model.Grid;

/**
 * Stores information that changes can rely on when modifying the project grid.
 * <p>
 * If we add multi-user support, we could for instance expose the user who committed the change, if we want this to let
 * this information influence the change.
 */
public interface ChangeContext {

    /**
     * Creates a change context for a given history entry id and a project id.
     */
    // TODO update javadoc
    static ChangeContext create(long historyEntryId, long projectId, int stepIndex, History history, ChangeDataStore dataStore,
            String description) {
        return new ChangeContextImpl(historyEntryId, projectId, stepIndex, history, dataStore, description);
    }

    long getHistoryEntryId();

    long getProjectId();

    String getChangeDescription();

    /**
     * Retrieves a {@link ChangeData} from the underlying {@link ChangeDataStore}. It must have been registered in the
     * store beforehand.
     *
     * @param completionProcess
     *            a function taking the grid to compute the change data on, the existing state of the change data and
     *            returning the complete version
     * @param dependencies
     *            the list of columns the completion process relies on, or null if it might rely on any column
     */
    <T> ChangeData<T> getChangeData(String dataId,
            ChangeDataSerializer<T> serializer,
            BiFunction<Grid, Optional<ChangeData<T>>, ChangeData<T>> completionProcess,
            List<ColumnId> dependencies,
            Engine.Mode engineMode) throws IOException;

}
