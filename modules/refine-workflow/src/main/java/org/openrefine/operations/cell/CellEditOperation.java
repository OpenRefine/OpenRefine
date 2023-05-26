
package org.openrefine.operations.cell;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.expr.ParsingException;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.Operation;
import org.openrefine.util.ParsingUtilities;

/**
 * Operation which edits a single cell in a given row and column.
 */
public class CellEditOperation implements Operation {

    private final long row;
    private final String columnName;
    private final Serializable newCellValue;
    private final String type;

    @JsonCreator
    public CellEditOperation(
            @JsonProperty("rowId") long row,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("newCellValue") Object newCellValue,
            @JsonProperty("type") String type) {
        this.row = row;
        this.columnName = columnName;
        Serializable serializable = (Serializable) newCellValue;
        if ("date".equals(type)) {
            serializable = ParsingUtilities.stringToDate(Objects.toString(newCellValue));
        }
        this.newCellValue = serializable;
        this.type = type;
    }

    @Override
    public Operation.ChangeResult apply(Grid projectState, ChangeContext context) throws ParsingException, Operation.DoesNotApplyException {
        int index = projectState.getColumnModel().getColumnIndexByName(columnName);
        if (index == -1) {
            throw new Operation.DoesNotApplyException(
                    String.format("Column '%s' does not exist", columnName));
        }
        ColumnModel columnModel = projectState.getColumnModel();
        boolean recordsPreserved = index != columnModel.getKeyColumnIndex();
        Grid result = projectState.mapRows(mapFunction(index, row, newCellValue, columnModel.getKeyColumnIndex()), columnModel);
        return new Operation.ChangeResult(
                result,
                recordsPreserved ? GridPreservation.PRESERVES_RECORDS : GridPreservation.PRESERVES_ROWS);
    }

    static protected RowMapper mapFunction(int cellIndex, long rowId, Serializable newCellValue, int keyColumnIndex) {
        return new RowMapper() {

            private static final long serialVersionUID = -5983834950609157341L;

            @Override
            public Row call(long currentRowId, Row row) {
                if (rowId == currentRowId) {
                    Cell oldCell = row.getCell(cellIndex);
                    Cell newCell = newCellValue == null ? null : new Cell(newCellValue, oldCell == null ? null : oldCell.recon);
                    return row.withCell(cellIndex, newCell);
                } else {
                    return row;
                }
            }

            @Override
            public boolean preservesRecordStructure() {
                return keyColumnIndex != cellIndex;
            }
        };
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

    @JsonProperty("type")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getType() {
        return type;
    }

}
