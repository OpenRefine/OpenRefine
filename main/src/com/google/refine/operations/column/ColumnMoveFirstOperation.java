
package com.google.refine.operations.column;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.model.Project;
import com.google.refine.operations.OperationDescription;

/**
 * Moves one column to the left-most position (first column of the project)
 */
public class ColumnMoveFirstOperation extends AbstractColumnMoveOperation {

    @JsonCreator
    public ColumnMoveFirstOperation(
            @JsonProperty("columnName") String columnName) {
        super(columnName);
    }

    @Override
    protected String getBriefDescription(Project project) {
        return OperationDescription.column_move_first_brief(_columnName);
    }

    @Override
    protected int getNewColumnIndex(int currentIndex, Project project) {
        return 0;
    }

    @Override
    public ColumnMoveFirstOperation renameColumns(Map<String, String> renames) {
        return new ColumnMoveFirstOperation(renames.getOrDefault(_columnName, _columnName));
    }
}
