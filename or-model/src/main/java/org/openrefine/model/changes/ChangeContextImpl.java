package org.openrefine.model.changes;

import java.io.IOException;
import java.io.Serializable;

public class ChangeContextImpl implements ChangeContext {
	
	private final long _historyEntryId;
	private final ChangeDataStore _dataStore;
	
	public ChangeContextImpl(long historyEntryId, ChangeDataStore dataStore) {
		_historyEntryId = historyEntryId;
		_dataStore = dataStore;
	}

	@Override
	public long getHistoryEntryId() {
		return _historyEntryId;
	}
	

    @Override
    public <T extends Serializable> ChangeData<T> getChangeData(String dataId, ChangeDataSerializer<T> serializer) throws IOException {
        return _dataStore.retrieve(_historyEntryId, dataId, serializer);
    }

	@Override
	public boolean equals(Object other) {
		if (! (other instanceof ChangeContext)) {
			return false;
		}
		return ((ChangeContext)other).getHistoryEntryId() == _historyEntryId;
	}
	
	@Override
	public int hashCode() {
		return (int) _historyEntryId;
	}
	
	@Override
	public String toString() {
		return String.format("[ChangeContext: %d]", _historyEntryId);
	}
	
}
