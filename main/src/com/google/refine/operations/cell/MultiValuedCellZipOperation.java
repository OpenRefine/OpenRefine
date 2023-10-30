package com.google.refine.operations.cell;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.*;
import com.google.refine.model.changes.MassRowChange;

import java.util.ArrayList;
import java.util.List;

public class MultiValuedCellZipOperation extends AbstractOperation {
    final protected String _columnName1;
    final protected String _columnName2;
    final protected String _resultColumnName;

    @JsonCreator
    public MultiValuedCellZipOperation(
            @JsonProperty("columnName1") String columnName1,
            @JsonProperty("columnName2") String columnName2,
            @JsonProperty("resultColumnName") String resultColumnName) {
        _columnName1 = columnName1;
        _columnName2 = columnName2;
        _resultColumnName = resultColumnName;
    }

    @JsonProperty("columnName1")
    public String getColumnName1() {
        return _columnName1;
    }

    @JsonProperty("columnName2")
    public String getColumnName2() {
        return _columnName2;
    }

    @JsonProperty("resultColumnName")
    public String getResultColumnName() {
        return _resultColumnName;
    }

    @Override
    protected String getBriefDescription(Project project) {
        return "Zip columns: " + _columnName1 + " and " + _columnName2;
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        // Find the target columns
        Column column1 = project.columnModel.getColumnByName(_columnName1);
        Column column2 = project.columnModel.getColumnByName(_columnName2);

        if (column1 == null || column2 == null) {
            throw new Exception("One or both columns not found");
        }

        int cellIndex1 = column1.getCellIndex();
        int cellIndex2 = column2.getCellIndex();

        List<Row> newRows = new ArrayList<Row>();

        int rowCount = project.rows.size();
        for (int r = 0; r < rowCount; r++) {
            Row oldRow = project.rows.get(r);
            Object value1 = oldRow.getCellValue(cellIndex1);
            Object value2 = oldRow.getCellValue(cellIndex2);

            if (value1 != null && value2 != null) {
                String resultValue = value1.toString() + value2.toString();
                Row newRow = oldRow.dup();
                newRow.setCell(cellIndex1, new Cell(resultValue, null));
                newRow.setCell(cellIndex2, null);
                newRows.add(newRow);
            }
        }

        return new HistoryEntry(
                historyEntryID,
                project,
                getBriefDescription(null),
                this,
                new MassRowChange(newRows));
    }

    @Override
    protected String createDescription(Column column) {
        return null;
    }
}