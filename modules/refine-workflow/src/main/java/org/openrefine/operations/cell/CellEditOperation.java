
package org.openrefine.operations.cell;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.expr.ParsingException;
import org.openrefine.model.changes.CellChange;
import org.openrefine.model.changes.Change;
import org.openrefine.operations.Operation;

/**
 * Operation which edits a single cell in a given row and column.
 */
public class CellEditOperation implements Operation {

    private final long row;
    private final String columnName;
    private final Serializable newCellValue;

    @JsonCreator
    public CellEditOperation(
            @JsonProperty("rowId") long row,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("newCellValue") Object newCellValue) {
        this.row = row;
        this.columnName = columnName;
        this.newCellValue = (Serializable) newCellValue;
    }

    @Override
    public Change createChange() throws ParsingException {
        return new CellChange(row, columnName, newCellValue);
    }

    @Override
    public String getDescription() {
        // TODO localize
        return "Edit single cell on row " + (row + 1) + ", column " + columnName;
    }

    /**
     * The 0-based id of the affected row.
     */
    @JsonProperty("rowId")
    public long getRowId() {
        return row;
    }

    /**
     * The name of the column where the cell is changed.
     */
    @JsonProperty("columnName")
    public String getColumnName() {
        return columnName;
    }

    /**
     * The new value of the cell to use.
     */
    @JsonProperty("newCellValue")
    public Serializable getNewCellValue() {
        return newCellValue;
    }

}
