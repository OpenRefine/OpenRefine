
package org.openrefine.model.changes;

import java.util.Objects;

/**
 * A pair of a history entry id and a string identifier for the change data in it.
 */
public class ChangeDataId {

    private final long historyEntryId;
    private final String subDirectory;

    public ChangeDataId(long historyEntryId, String subDirectory) {
        this.historyEntryId = historyEntryId;
        this.subDirectory = subDirectory;
    }

    public long getHistoryEntryId() {
        return historyEntryId;
    }

    public String getSubDirectory() {
        return subDirectory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChangeDataId that = (ChangeDataId) o;
        return historyEntryId == that.historyEntryId && Objects.equals(subDirectory, that.subDirectory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(historyEntryId, subDirectory);
    }

    @Override
    public String toString() {
        return "ChangeDataId{" +
                "historyEntryId=" + historyEntryId +
                ", subDirectory='" + subDirectory + '\'' +
                '}';
    }
}
