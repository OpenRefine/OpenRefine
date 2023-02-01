
package org.openrefine.model.changes;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

public class ChangeContextImpl implements ChangeContext {

    private final long _historyEntryId;
    private final long _projectId;
    private final ChangeDataStore _dataStore;
    private final String _changeDescription;

    public ChangeContextImpl(long historyEntryId, long projectId, ChangeDataStore dataStore, String description) {
        _historyEntryId = historyEntryId;
        _projectId = projectId;
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
    public <T> ChangeData<T> getChangeData(String dataId, ChangeDataSerializer<T> serializer,
            Function<Optional<ChangeData<T>>, ChangeData<T>> completionProcess) throws IOException {
        return _dataStore.retrieveOrCompute(new ChangeDataId(_historyEntryId, dataId), serializer, completionProcess, _changeDescription);
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
