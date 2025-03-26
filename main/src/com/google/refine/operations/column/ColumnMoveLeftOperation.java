
package com.google.refine.operations.column;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.model.Project;
import com.google.refine.operations.OperationDescription;

/**
 * Moves one column to the left.
 */
public class ColumnMoveLeftOperation extends AbstractColumnMoveOperation {

    @JsonCreator
    public ColumnMoveLeftOperation(
            @JsonProperty("columnName") String columnName) {
        super(columnName);
    }

    @Override
    protected String getBriefDescription(Project project) {
        return OperationDescription.column_move_left_brief(_columnName);
    }

    @Override
    protected int getNewColumnIndex(int currentIndex, Project project) {
        return Math.max(0, currentIndex - 1);
    }

    @Override
    public ColumnMoveLeftOperation renameColumns(Map<String, String> renames) {
        return new ColumnMoveLeftOperation(renames.getOrDefault(_columnName, _columnName));
    }
}
