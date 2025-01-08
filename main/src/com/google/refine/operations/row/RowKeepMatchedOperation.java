
package com.google.refine.operations.row;

import static com.google.refine.operations.OperationDescription.row_keep_matching_brief;
import static com.google.refine.operations.OperationDescription.row_keep_matching_rows_desc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.RowRemovalChange;
import com.google.refine.operations.EngineDependentOperation;

public class RowKeepMatchedOperation extends EngineDependentOperation {

    @JsonCreator
    public RowKeepMatchedOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig) {
        super(engineConfig);
    }

    @Override
    protected String getBriefDescription(Project project) {
        return row_keep_matching_brief();

    }
    protected String createDescription(Project project, int rowCount) {
        return String.format("%s (%s)",
                row_keep_matching_brief(),
                row_keep_matching_rows_desc(rowCount)
        );
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        Engine engine = createEngine(project);
        FilteredRows filteredRows = engine.getAllFilteredRows();

        Set<Integer> matchedRows = new HashSet<>();

        filteredRows.accept(project, new RowVisitor() {

            @Override
            public void start(Project project) {
            }

            @Override
            public void end(Project project) {
            }

            @Override
            public boolean visit(Project project, int rowIndex, Row row) {
                matchedRows.add(rowIndex);
                return false;
            }
        });
        List<Integer> rowsToRemove = new ArrayList<>(project.rows.size() - matchedRows.size());
        for (int i = 0; i < project.rows.size(); i++) {
            if (!matchedRows.contains(i)) {
                rowsToRemove.add(i);
            }
        }
        return new HistoryEntry(
                historyEntryID,
                project,
                createDescription(project, rowsToRemove.size()),
                this,
                new RowRemovalChange(rowsToRemove));
    }
}
