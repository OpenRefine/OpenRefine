package org.openrefine.model.changes;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.openrefine.model.DatamodelRunner;

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
        return new File(new File(_baseDirectory, Long.toString(historyEntryId)), dataId);
    }

    @Override
    public <T extends Serializable> void store(ChangeData<T> data, long historyEntryId, String dataId,
            ChangeDataSerializer<T> serializer) throws IOException {
        File file = idsToFile(historyEntryId, dataId);
        file.mkdirs();
        data.saveToFile(file, serializer);
    }

    @Override
    public <T extends Serializable> ChangeData<T> retrieve(long historyEntryId, String dataId,
            ChangeDataSerializer<T> serializer) throws IOException {
        File file = idsToFile(historyEntryId, dataId);
        return _runner.loadChangeData(file, serializer);
    }

}
