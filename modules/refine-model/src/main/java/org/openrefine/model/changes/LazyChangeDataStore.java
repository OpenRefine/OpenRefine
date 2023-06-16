
package org.openrefine.model.changes;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.openrefine.model.Runner;
import org.openrefine.process.ProcessManager;
import org.openrefine.process.ProgressReporter;
import org.openrefine.process.ProgressingFuture;
import org.openrefine.process.ProgressingFutures;

/**
 * A {@link ChangeDataStore} which does not persist its change data, meaning that they do not have to be computed
 * explicitly.
 * <p>
 * The {@link ChangeData} objects are simply stored in an in-memory map, to be restored later. This is suitable in
 * contexts where project data must not be persisted and workflows are meant to be run as pipelines from the import to
 * the export stage.
 */
public class LazyChangeDataStore implements ChangeDataStore {

    private final Map<ChangeDataId, ChangeData<?>> _changeData;
    private final Runner _runner;
    private final ProcessManager processManager = new ProcessManager();

    public LazyChangeDataStore(Runner runner) {
        _changeData = new HashMap<>();
        _runner = runner;
    }

    @Override
    public ProcessManager getProcessManager() {
        return processManager;
    }

    @Override
    public <T> void store(ChangeData<T> data, ChangeDataId changeDataId,
            ChangeDataSerializer<T> serializer, Optional<ProgressReporter> progressReporter) throws IOException {
        _changeData.put(changeDataId, data);
    }

    @Override
    public <T> ProgressingFuture<Void> storeAsync(ChangeData<T> data, ChangeDataId changeDataId, ChangeDataSerializer<T> serializer) {
        _changeData.put(changeDataId, data);
        return ProgressingFutures.immediate(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ChangeData<T> retrieve(ChangeDataId changeDataId,
            ChangeDataSerializer<T> serializer) throws IOException {
        if (!_changeData.containsKey(changeDataId)) {
            throw new IllegalArgumentException(String.format("Change data with id %s does not exist", changeDataId.toString()));
        }
        return (ChangeData<T>) _changeData.get(changeDataId);
    }

    @Override
    public <T> ChangeData<T> retrieveOrCompute(
            ChangeDataId changeDataId,
            ChangeDataSerializer<T> serializer,
            Function<Optional<ChangeData<T>>, ChangeData<T>> completionProcess, String description) throws IOException {
        if (!_changeData.containsKey(changeDataId)) {
            ChangeData<T> computed = completionProcess.apply(Optional.empty());
            _changeData.put(changeDataId, computed);
        }
        return (ChangeData<T>) _changeData.get(changeDataId);
    }

    @Override
    public boolean needsRefreshing(long historyEntryId) {
        return false;
    }

    @Override
    public void discardAll(long historyEntryId) {
        for (ChangeDataId id : getChangeDataIds(historyEntryId)) {
            _changeData.remove(id);
        }
    }

    @Override
    public List<ChangeDataId> getChangeDataIds(long historyEntryId) {
        return _changeData.keySet().stream()
                .filter(id -> id.getHistoryEntryId() == historyEntryId)
                .collect(Collectors.toList());
    }

}
