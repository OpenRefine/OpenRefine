
package org.openrefine.model.changes;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import org.openrefine.model.Runner;
import org.openrefine.process.Process;
import org.openrefine.process.ProcessManager;
import org.openrefine.process.ProgressReporter;

public class FileChangeDataStore implements ChangeDataStore {

    private final Runner _runner;
    private final File _baseDirectory;
    private final ProcessManager processManager = new ProcessManager();
    private final Set<ChangeDataId> _toRefresh;

    public FileChangeDataStore(Runner runner, File baseDirectory) {
        _runner = runner;
        _baseDirectory = baseDirectory;
        _toRefresh = new HashSet<>();
    }

    /**
     * Associates to a pair of ids the location where we should store them.
     */
    private File idsToFile(ChangeDataId changeDataId) {
        return new File(historyEntryIdToFile(changeDataId.getHistoryEntryId()), changeDataId.getSubDirectory());
    }

    private Set<ChangeDataId> changeDataIdsInProgress() {
        return processManager.getProcesses().stream()
                .map(Process::getChangeDataId)
                .collect(Collectors.toSet());
    }

    /**
     * Directory where all change data belonging to a given history entry id should be stored.
     */
    private File historyEntryIdToFile(long historyEntryId) {
        return new File(_baseDirectory, Long.toString(historyEntryId));
    }

    @Override
    public ProcessManager getProcessManager() {
        return processManager;
    }

    @Override
    public <T> void store(ChangeData<T> data, ChangeDataId changeDataId,
            ChangeDataSerializer<T> serializer, Optional<ProgressReporter> progressReporter) throws IOException {
        File file = idsToFile(changeDataId);
        file.mkdirs();
        try {
            if (progressReporter.isPresent()) {
                data.saveToFile(file, serializer, progressReporter.get());
            } else {
                data.saveToFile(file, serializer);
            }
        } catch (InterruptedException e) {
            FileUtils.deleteDirectory(file);
            throw new IOException(e);
        }
    }

    @Override
    public <T> ChangeData<T> retrieve(ChangeDataId changeDataId,
            ChangeDataSerializer<T> serializer) throws IOException {
        File file = idsToFile(changeDataId);
        ChangeData<T> changeData = _runner.loadChangeData(file, serializer);
        if (changeData.isComplete() && _toRefresh.contains(changeDataId)) {
            _toRefresh.remove(changeDataId);
        }
        return changeData;
    }

    @Override
    public <T> ChangeData<T> retrieveOrCompute(
            ChangeDataId changeDataId,
            ChangeDataSerializer<T> serializer,
            Function<Optional<ChangeData<T>>, ChangeData<T>> completionProcess, String description) throws IOException {
        File file = idsToFile(changeDataId);

        Optional<ChangeData<T>> storedChangeData;
        boolean storedChangedDataIsComplete;
        try {
            storedChangeData = Optional.of(_runner.loadChangeData(file, serializer));
            storedChangedDataIsComplete = storedChangeData.get().isComplete();
        } catch (IOException e) {
            storedChangeData = Optional.empty();
            storedChangedDataIsComplete = false;
        }

        if (!storedChangedDataIsComplete && !changeDataIdsInProgress().contains(changeDataId)) {
            // queue a new process to compute the change data
            processManager.queueProcess(new ChangeDataStoringProcess<T>(description,
                    storedChangeData,
                    changeDataId,
                    this,
                    serializer,
                    completionProcess));
            _toRefresh.add(changeDataId);
        }
        return storedChangeData.orElse(_runner.create(Collections.emptyList()));
    }

    @Override
    public boolean needsRefreshing(long historyEntryId) {
        return _toRefresh.stream().map(ChangeDataId::getHistoryEntryId).anyMatch(id -> id == historyEntryId);
    }

    @Override
    public void discardAll(long historyEntryId) {
        File file = historyEntryIdToFile(historyEntryId);
        if (file.exists()) {
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                ;
            }
        }
    }

    protected static class ChangeDataStoringProcess<T> extends Process implements Runnable {

        final Optional<ChangeData<T>> storedChangeData;
        final ChangeDataId changeDataId;
        final ChangeDataStore changeDataStore;
        final ChangeDataSerializer<T> serializer;
        final Function<Optional<ChangeData<T>>, ChangeData<T>> completionProcess;

        public ChangeDataStoringProcess(
                String description,
                Optional<ChangeData<T>> storedChangeData,
                ChangeDataId changeDataId,
                ChangeDataStore changeDataStore,
                ChangeDataSerializer<T> serializer, Function<Optional<ChangeData<T>>, ChangeData<T>> completionProcess) {
            super(description);
            this.storedChangeData = storedChangeData;
            this.changeDataId = changeDataId;
            this.changeDataStore = changeDataStore;
            this.serializer = serializer;
            this.completionProcess = completionProcess;
        }

        @Override
        protected Runnable getRunnable() {
            return this;
        }

        @Override
        public ChangeDataId getChangeDataId() {
            return changeDataId;
        }

        @Override
        public void run() {
            ChangeData<T> newChangeData = completionProcess.apply(storedChangeData);
            try {
                changeDataStore.store(newChangeData, changeDataId, serializer, Optional.of(_reporter));
                _manager.onDoneProcess(this);
            } catch (Exception e) {
                _manager.onFailedProcess(this, e);
            }
        }
    }

}
