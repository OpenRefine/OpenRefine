
package org.openrefine.model.changes;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

import org.apache.commons.io.FileUtils;

import org.openrefine.model.DatamodelRunner;
import org.openrefine.process.ProgressReporter;

public class FileChangeDataStore implements ChangeDataStore {

    private DatamodelRunner _runner;
    private File _baseDirectory;

    public FileChangeDataStore(DatamodelRunner runner, File baseDirectory) {
        _runner = runner;
        _baseDirectory = baseDirectory;
    }

    /**
     * Associates to a pair of ids the location where we should store them.
     * 
     * @param historyEntryId
     * @param dataId
     * @return
     */
    private File idsToFile(long historyEntryId, String dataId) {
        return new File(historyEntryIdToFile(historyEntryId), dataId);
    }

    /**
     * Directory where all change data belonging to a given history entry id should be stored.
     */
    private File historyEntryIdToFile(long historyEntryId) {
        return new File(_baseDirectory, Long.toString(historyEntryId));
    }

    @Override
    public <T extends Serializable> void store(ChangeData<T> data, long historyEntryId, String dataId,
            ChangeDataSerializer<T> serializer, Optional<ProgressReporter> progressReporter) throws IOException {
        File file = idsToFile(historyEntryId, dataId);
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
    public <T extends Serializable> ChangeData<T> retrieve(long historyEntryId, String dataId,
            ChangeDataSerializer<T> serializer) throws IOException {
        File file = idsToFile(historyEntryId, dataId);
        return _runner.loadChangeData(file, serializer);
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

}
