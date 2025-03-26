
package com.google.refine.operations.column;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.model.Project;
import com.google.refine.operations.OperationDescription;

/**
 * Moves a column to the end (last column of the project).
 */
public class ColumnMoveLastOperation extends AbstractColumnMoveOperation {

    @JsonCreator
    public ColumnMoveLastOperation(
            @JsonProperty("columnName") String columnName) {
        super(columnName);
    }

    @Override
    protected String getBriefDescription(Project project) {
        return OperationDescription.column_move_last_brief(_columnName);
    }

    @Override
    protected int getNewColumnIndex(int currentIndex, Project project) {
        return project.columnModel.columns.size() - 1;
    }

    @Override
    public ColumnMoveLastOperation renameColumns(Map<String, String> renames) {
        return new ColumnMoveLastOperation(renames.getOrDefault(_columnName, _columnName));
    }
}
