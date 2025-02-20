
package com.google.refine.operations.column;

import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.Validate;

import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;
import com.google.refine.model.changes.ColumnMoveChange;

/**
 * Base class for operations moving a single column to a new position.
 */
abstract class AbstractColumnMoveOperation extends AbstractOperation {

    final protected String _columnName;

    public AbstractColumnMoveOperation(
            String columnName) {
        _columnName = columnName;
    }

    @Override
    public void validate() {
        Validate.notNull(_columnName, "Missing column name");
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    protected abstract int getNewColumnIndex(int currentIndex, Project project);

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        int index = project.columnModel.getColumnIndexByName(_columnName);
        if (index == -1) {
            throw new Exception("No column named " + _columnName);
        }
        int newIndex = getNewColumnIndex(index, project);

        Change change = new ColumnMoveChange(_columnName, newIndex);

        return new HistoryEntry(historyEntryID, project, getBriefDescription(null), this, change);
    }

    @Override
    public Optional<Set<String>> getColumnDependencies() {
        return Optional.of(Set.of(_columnName));
    }

    @Override
    public Optional<ColumnsDiff> getColumnsDiff() {
        return Optional.of(ColumnsDiff.empty());
    }
}
