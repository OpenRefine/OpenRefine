
package org.openrefine.model.changes;

public class ChangeContextImpl implements ChangeContext {

    private final long _historyEntryId;

    public ChangeContextImpl(long historyEntryId) {
        _historyEntryId = historyEntryId;
    }

    @Override
    public long getHistoryEntryId() {
        return _historyEntryId;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ChangeContext)) {
            return false;
        }
        return ((ChangeContext) other).getHistoryEntryId() == _historyEntryId;
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
