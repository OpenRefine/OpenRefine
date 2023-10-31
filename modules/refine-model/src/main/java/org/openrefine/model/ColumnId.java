
package org.openrefine.model;

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.Validate;

/**
 * This class represents the state of a column at a particular point in the history. If the column is not modified in
 * the subsequent operations, its {@link ColumnMetadata} is able to indicate so by retaining the history entry id as its
 * {@link ColumnMetadata#getLastModified()} field and the column name as its {@link ColumnMetadata#getOriginalName()}.
 */
public class ColumnId implements Serializable {

    protected final String columnName;
    protected final long historyEntryId;

    public ColumnId(String columnName, long historyEntryId) {
        Validate.notNull(columnName);
        this.columnName = columnName;
        this.historyEntryId = historyEntryId;
    }

    public String getColumnName() {
        return columnName;
    }

    public long getHistoryEntryId() {
        return historyEntryId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColumnId that = (ColumnId) o;
        return historyEntryId == that.historyEntryId && columnName.equals(that.columnName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnName, historyEntryId);
    }

    @Override
    public String toString() {
        return "ColumnId{" +
                "'" + columnName + '\'' +
                ", " + historyEntryId + '}';
    }
}
