
package org.openrefine.model.changes;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.browsing.Engine;
import org.openrefine.history.History;
import org.openrefine.model.ColumnId;
import org.openrefine.model.Grid;
import org.openrefine.operations.exceptions.OperationException;

public class ChangeContextImpl implements ChangeContext {

    private final Logger logger = LoggerFactory.getLogger(ChangeContext.class);

    private final long _historyEntryId;
    private final long _projectId;
    private final int _stepIndex;
    private final History _history;
    private final ChangeDataStore _dataStore;
    private final String _changeDescription;

    public ChangeContextImpl(long historyEntryId, long projectId, int stepIndex, History history, ChangeDataStore dataStore,
            String description) {
        _historyEntryId = historyEntryId;
        _projectId = projectId;
        _stepIndex = stepIndex;
        _history = history;
        _dataStore = dataStore;
        _changeDescription = description;
    }

    @Override
    public long getHistoryEntryId() {
        return _historyEntryId;
    }

    @Override
    public long getProjectId() {
        return _projectId;
    }

    @Override
    public String getChangeDescription() {
        return _changeDescription;
    }

    @Override
    public <T> ChangeData<T> getChangeData(
            String dataId,
            ChangeDataSerializer<T> serializer,
            BiFunction<Grid, Optional<ChangeData<T>>, ChangeData<T>> completionProcess,
            List<ColumnId> dependencies,
            Engine.Mode mode) throws IOException {
        int applicationIndex = _history.earliestStepContainingDependencies(_stepIndex, dependencies, mode);
        logger.debug("Earliest step containing dependencies is {} for {}", applicationIndex, dependencies);
        try {
            Grid grid = _history.getGrid(applicationIndex, false);
            return _dataStore.retrieveOrCompute(
                    new ChangeDataId(_historyEntryId, dataId),
                    serializer,
                    grid,
                    partialChangeData -> completionProcess.apply(grid, partialChangeData),
                    _changeDescription,
                    _history, applicationIndex, mode);
        } catch (OperationException e) {
            // unreachable since earliestStepContainingDependencies must return a grid that is already computed
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ChangeContext)) {
            return false;
        }
        return ((ChangeContext) other).getHistoryEntryId() == _historyEntryId &&
                ((ChangeContext) other).getProjectId() == _projectId;
    }

    @Override
    public int hashCode() {
        return (int) _historyEntryId;
    }

    @Override
    public String toString() {
        return String.format("[ChangeContext: %d, %d]", _historyEntryId, _projectId);
    }

}
