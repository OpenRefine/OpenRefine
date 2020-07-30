package org.openrefine.model.changes;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link ChangeDataStore} which does not persist its change data,
 * meaning that they do not have to be computed explicitly.
 * 
 * The {@link ChangeData} objects are simply stored in an in-memory map,
 * to be restored later. This is suitable in contexts where project data
 * must not be persisted and workflows are meant to be run as pipelines
 * from the import to the export stage.
 * 
 * @author Antonin Delpeuch
 *
 */
public class LazyChangeDataStore implements ChangeDataStore {
    
    private Map<String, ChangeData<?>> _changeData;
    
    public LazyChangeDataStore() {
        _changeData = new HashMap<>();
    }
    
    private String idPairToString(long historyEntryId, String dataId) {
        return String.format("%d/%s", historyEntryId, dataId);
    }

    @Override
    public <T extends Serializable> void store(ChangeData<T> data, long historyEntryId, String dataId,
            ChangeDataSerializer<T> serializer) throws IOException {
        _changeData.put(idPairToString(historyEntryId, dataId), data);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Serializable> ChangeData<T> retrieve(long historyEntryId, String dataId,
            ChangeDataSerializer<T> serializer) throws IOException {
        String key = idPairToString(historyEntryId, dataId);
        if (!_changeData.containsKey(key)) {
            throw new IllegalArgumentException(String.format("Change data with id %s does not exist", key));
        }
        return (ChangeData<T>)_changeData.get(key);
    }
    
}
