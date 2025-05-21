
package com.google.refine.operations.column;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.Validate;

import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;
import com.google.refine.model.changes.ColumnReorderChange;
import com.google.refine.operations.OperationDescription;

/**
 * Removes multiple columns in a single step.
 */
public class ColumnMultiRemovalOperation extends AbstractOperation {

    final protected List<String> _columnNames;
    final protected Set<String> _columnNameSet;

    @JsonCreator
    public ColumnMultiRemovalOperation(
            @JsonProperty("columnNames") List<String> columnNames) {
        _columnNames = columnNames;
        _columnNameSet = columnNames == null ? null : _columnNames.stream().collect(Collectors.toSet());
    }

    @Override
    public void validate() {
        Validate.notNull(_columnNames, "Missing column names");
    }

    @JsonProperty("columnNames")
    public List<String> getColumnNames() {
        return _columnNames;
    }

    @Override
    protected String getBriefDescription(Project project) {
        return OperationDescription.column_multi_removal_brief(_columnNames.size());
    }

    @Override
    public Optional<Set<String>> getColumnDependencies() {
        return Optional.of(_columnNameSet);
    }

    @Override
    public Optional<ColumnsDiff> getColumnsDiff() {
        return Optional.of(new ColumnsDiff(List.of(), _columnNameSet, Set.of()));
    }

    @Override
    public ColumnMultiRemovalOperation renameColumns(Map<String, String> newColumnNames) {
        List<String> renamed = _columnNames.stream()
                .map(old -> newColumnNames.getOrDefault(old, old))
                .collect(Collectors.toList());
        return new ColumnMultiRemovalOperation(renamed);
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        List<String> remainingColumnNames = project.columnModel.getColumnNames()
                .stream()
                .filter(name -> !_columnNameSet.contains(name))
                .collect(Collectors.toList());
        return new HistoryEntry(
                historyEntryID,
                project,
                getBriefDescription(project),
                this,
                new ColumnReorderChange(remainingColumnNames));
    }
}
