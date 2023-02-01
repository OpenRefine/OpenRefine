
package org.openrefine.model.changes;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import org.openrefine.model.Runner;
import org.openrefine.process.ProcessManager;
import org.openrefine.process.ProgressReporter;

/**
 * A {@link ChangeDataStore} which does not persist its change data, meaning that they do not have to be computed
 * explicitly.
 * <p>
 * The {@link ChangeData} objects are simply stored in an in-memory map, to be restored later. This is suitable in
 * contexts where project data must not be persisted and workflows are meant to be run as pipelines from the import to
 * the export stage.
 */
public class LazyChangeDataStore implements ChangeDataStore {

    private final Map<String, ChangeData<?>> _changeData;
    private final Runner _runner;
    private final ProcessManager processManager = new ProcessManager();

    public LazyChangeDataStore(Runner runner) {
        _changeData = new HashMap<>();
        _runner = runner;
    }

    private String idPairToString(ChangeDataId changeDataId) {
        return String.format("%d/%s", changeDataId.getHistoryEntryId(), changeDataId.getSubDirectory());
    }

    @Override
    public ProcessManager getProcessManager() {
        return processManager;
    }

    @Override
    public <T> void store(ChangeData<T> data, ChangeDataId changeDataId,
            ChangeDataSerializer<T> serializer, Optional<ProgressReporter> progressReporter) throws IOException {
        _changeData.put(idPairToString(changeDataId), data);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ChangeData<T> retrieve(ChangeDataId changeDataId,
            ChangeDataSerializer<T> serializer) throws IOException {
        String key = idPairToString(changeDataId);
        if (!_changeData.containsKey(key)) {
            throw new IllegalArgumentException(String.format("Change data with id %s does not exist", key));
        }
        return (ChangeData<T>) _changeData.get(key);
    }

    @Override
    public <T> ChangeData<T> retrieveOrCompute(
            ChangeDataId changeDataId,
            ChangeDataSerializer<T> serializer,
            Function<Optional<ChangeData<T>>, ChangeData<T>> completionProcess, String description) throws IOException {
        String key = idPairToString(changeDataId);
        if (!_changeData.containsKey(key)) {
            ChangeData<T> computed = completionProcess.apply(Optional.empty());
            _changeData.put(key, computed);
        }
        return (ChangeData<T>) _changeData.get(key);
    }

    @Override
    public boolean needsRefreshing(long historyEntryId) {
        return false;
    }

    @Override
    public void discardAll(long historyEntryId) {
        Iterator<String> keySet = _changeData.keySet().iterator();
        while (keySet.hasNext()) {
            String key = keySet.next();
            if (key.startsWith(Long.toString(historyEntryId) + "/")) {
                keySet.remove();
            }
        }
    }

}
