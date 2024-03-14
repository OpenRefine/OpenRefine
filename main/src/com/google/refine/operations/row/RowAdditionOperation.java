
package com.google.refine.operations.row;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.RowAdditionChange;
import com.google.refine.operations.OperationDescription;

public class RowAdditionOperation extends AbstractOperation {

    final private List<Row> _rows;

    @JsonCreator
    public RowAdditionOperation(
            @JsonProperty("rows") List<Row> rows) {
        _rows = rows;
    }

    @Override
    protected String getBriefDescription(Project project) {
        return OperationDescription.row_addition_brief();
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        int insertionIndex = 0; // Prepend rows

        int count = _rows.size();
        String description = "Add " + count + " row" + ((count > 1) ? "s" : "");

        return new HistoryEntry(
                historyEntryID,
                project,
                description,
                this,
                new RowAdditionChange(_rows, insertionIndex));
    }

}
