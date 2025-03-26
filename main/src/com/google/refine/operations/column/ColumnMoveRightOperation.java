
package com.google.refine.operations.column;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.model.Project;
import com.google.refine.operations.OperationDescription;

/**
 * Moves one column to the right.
 */
public class ColumnMoveRightOperation extends AbstractColumnMoveOperation {

    @JsonCreator
    public ColumnMoveRightOperation(
            @JsonProperty("columnName") String columnName) {
        super(columnName);
    }

    @Override
    protected String getBriefDescription(Project project) {
        return OperationDescription.column_move_right_brief(_columnName);
    }

    @Override
    protected int getNewColumnIndex(int currentIndex, Project project) {
        return Math.min(project.columnModel.columns.size() - 1, currentIndex + 1);
    }

    @Override
    public ColumnMoveRightOperation renameColumns(Map<String, String> renames) {
        return new ColumnMoveRightOperation(renames.getOrDefault(_columnName, _columnName));
    }
}
