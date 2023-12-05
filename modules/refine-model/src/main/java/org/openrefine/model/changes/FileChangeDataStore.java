
package org.openrefine.model.changes;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.history.History;
import org.openrefine.model.Grid;
import org.openrefine.model.Runner;
import org.openrefine.process.*;
import org.openrefine.process.Process;

/**
 * A {@link ChangeDataStore} which stores change data on disk. This is the default one used in OpenRefine. <br>
 * It makes use of two directories:
 * <ul>
 * <li>The base directory, where changes are stored;</li>
 * <li>The incomplete directory, which is used as a temporary location when resuming the fetching of some change data
 * after an interruption.</li>
 * </ul>
 * When {@link #retrieveOrCompute(ChangeDataId, ChangeDataSerializer, Grid, Function, String, History, int)} finds an
 * incomplete change data is found in the base directory, it is moved to the incomplete directory. A new version of the
 * change data, completed using the completion process, is then saved again in the base directory.
 */
public class FileChangeDataStore implements ChangeDataStore {

    private final Logger logger = LoggerFactory.getLogger(FileChangeDataStore.class);

    private final Runner _runner;
    private final File _baseDirectory;
    private final File _incompleteDirectory;
    private final ProcessManager processManager = new ProcessManager();
    private final Set<ChangeDataId> _toRefresh;
    private final Map<Long, List<ChangeDataId>> _changeIds;

    public FileChangeDataStore(Runner runner, File baseDirectory, File incompleteDirectory) {
        _runner = runner;
        _baseDirectory = baseDirectory;
        _incompleteDirectory = incompleteDirectory;
        _toRefresh = new HashSet<>();
        _changeIds = new HashMap<>();
    }

    /**
     * Associates to a pair of ids the location where we should store them.
     */
    private File idsToFile(ChangeDataId changeDataId, boolean incomplete) {
        return new File(historyEntryIdToDir(changeDataId.getHistoryEntryId(), incomplete), changeDataId.getSubDirectory());
    }

    private Set<ChangeDataId> changeDataIdsInProgress() {
        return processManager.getProcesses().stream()
                .map(Process::getChangeDataId)
                .collect(Collectors.toSet());
    }

    /**
     * Directory where all change data belonging to a given history entry id should be stored.
     */
    private File historyEntryIdToDir(long historyEntryId, boolean incomplete) {
        return new File(incomplete ? _incompleteDirectory : _baseDirectory, Long.toString(historyEntryId));
    }

    @Override
    public ProcessManager getProcessManager() {
        return processManager;
    }

    @Override
    public <T> void store(ChangeData<T> data, ChangeDataId changeDataId,
            ChangeDataSerializer<T> serializer, Optional<ProgressReporter> progressReporter) throws IOException {
        File file = idsToFile(changeDataId, false);
        registerId(changeDataId);
        file.mkdirs();
        try {
            ProgressingFuture<Void> future = data.saveToFileAsync(file, serializer);
            if (progressReporter.isPresent()) {
                future.onProgress(progressReporter.get());
            }
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            FileUtils.deleteDirectory(file);
            throw new IOException(e);
        }
    }

    @Override
    public <T> ProgressingFuture<Void> storeAsync(ChangeData<T> data, ChangeDataId changeDataId,
            ChangeDataSerializer<T> serializer) {
        File file = idsToFile(changeDataId, false);
        registerId(changeDataId);
        file.mkdirs();
        return data.saveToFileAsync(file, serializer);
    }

    @Override
    public <T> ChangeData<T> retrieve(ChangeDataId changeDataId,
            ChangeDataSerializer<T> serializer) throws IOException {
        File file = idsToFile(changeDataId, false);
        ChangeData<T> changeData = _runner.loadChangeData(file, serializer, false);
        if (changeData.isComplete() && _toRefresh.contains(changeDataId)) {
            _toRefresh.remove(changeDataId);
        }
        registerId(changeDataId);
        return changeData;
    }

    @Override
    public <T> ChangeData<T> retrieveOrCompute(
            ChangeDataId changeDataId,
            ChangeDataSerializer<T> serializer,
            Grid baseGrid,
            Function<Optional<ChangeData<T>>, ChangeData<T>> completionProcess,
            String description,
            History history, int requiredStepIndex) throws IOException {
        File changeDataDir = idsToFile(changeDataId, false);
        registerId(changeDataId);
        File incompleteDir = null;

        Optional<ChangeData<T>> returnedChangeData;
        Optional<ChangeData<T>> recoveredChangeData;
        boolean storedChangeDataIsComplete;
        try {
            recoveredChangeData = Optional.of(_runner.loadChangeData(changeDataDir, serializer, false));
            storedChangeDataIsComplete = recoveredChangeData.get().isComplete();
        } catch (IOException e) {
            recoveredChangeData = Optional.empty();
            storedChangeDataIsComplete = false;
        }

        returnedChangeData = recoveredChangeData;

        if (!storedChangeDataIsComplete && !changeDataIdsInProgress().contains(changeDataId)) {
            if (recoveredChangeData.isPresent() && changeDataDir.exists()) {
                // move the existing change data to a temporary directory
                incompleteDir = idsToFile(changeDataId, true);
                logger.info("Recovering incomplete change data to temporary directory " + incompleteDir.toString());
                if (incompleteDir.exists()) {
                    FileUtils.deleteDirectory(incompleteDir);
                }
                FileUtils.moveDirectory(changeDataDir, incompleteDir);
                recoveredChangeData = Optional.of(_runner.loadChangeData(incompleteDir, serializer, true));
                returnedChangeData = Optional.empty();
            }

            if (returnedChangeData.isEmpty()) {
                logger.info("Writing new empty change data in directory " + changeDataDir.toString());
                // ensure the directory of the change data is already created at this stage
                ChangeData<T> emptyChangeData = baseGrid.emptyChangeData();
                try {
                    emptyChangeData.saveToFile(changeDataDir, serializer);
                } catch (Exception e) {
                    throw new IOException(e);
                }
                logger.info("Wrote the empty change data");
                returnedChangeData = Optional.of(_runner.loadChangeData(changeDataDir, serializer, false));
            }
            // queue a new process to compute the change data
            ChangeDataStoringProcess<T> process = new ChangeDataStoringProcess<T>(description,
                    recoveredChangeData,
                    changeDataId,
                    this,
                    serializer,
                    completionProcess,
                    incompleteDir,
                    history,
                    requiredStepIndex);
            processManager.queueProcess(process);
            _toRefresh.add(changeDataId);
        } else if (storedChangeDataIsComplete) {
            _toRefresh.remove(changeDataId);
        }
        return returnedChangeData.orElse(_runner.emptyChangeData());
    }

    protected void registerId(ChangeDataId id) {
        long historyEntryId = id.getHistoryEntryId();
        List<ChangeDataId> currentIds = getChangeDataIds(historyEntryId);
        if (!currentIds.contains(id)) {
            List<ChangeDataId> added = new LinkedList<>(currentIds);
            added.add(id);
            _changeIds.put(historyEntryId, added);
        }
    }

    @Override
    public boolean needsRefreshing(long historyEntryId) {
        return _toRefresh.stream()
                .map(ChangeDataId::getHistoryEntryId)
                .anyMatch(id -> id == historyEntryId);
    }

    @Override
    public List<ChangeDataId> getChangeDataIds(long historyEntryId) {
        List<ChangeDataId> ids = _changeIds.get(historyEntryId);
        return ids != null ? ids : Collections.emptyList();
    }

    @Override
    public void discardAll(long historyEntryId) {
        // first, cancel any processes which are fetching change data in this directory
        getProcessManager().getProcesses()
                .stream()
                .filter(p -> p.getChangeDataId().getHistoryEntryId() == historyEntryId)
                .forEach(process -> process.cancel());
        processManager.update();

        // then delete the directory and all subdirectories
        File file = historyEntryIdToDir(historyEntryId, false);
        if (file.exists()) {
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                ;
            }
        }
        // and unregister the corresponding ids
        _changeIds.remove(historyEntryId);
    }

    @Override
    public void dispose() {
        processManager.shutdown();
    }

}
