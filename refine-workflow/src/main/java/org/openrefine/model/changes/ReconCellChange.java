
package org.openrefine.model.changes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.ReconStats;

public class ReconCellChange implements Change {

    @JsonProperty("rowId")
    final protected long row;
    @JsonProperty("columnName")
    final protected String columnName;
    @JsonProperty("newRecon")
    final protected Recon newRecon;
    @JsonProperty("newReconStats")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    final protected ReconStats newReconStats;

    @JsonCreator
    public ReconCellChange(
            @JsonProperty("rowId") long rowId,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("newRecon") Recon newRecon,
            @JsonProperty("newReconStats") ReconStats newReconStats) {
        this.row = rowId;
        this.columnName = columnName;
        this.newRecon = newRecon;
        this.newReconStats = newReconStats;
    }

    @Override
    public GridState apply(GridState state, ChangeContext context) throws DoesNotApplyException {
        int columnIndex = state.getColumnModel().getColumnIndexByName(columnName);
        if (columnIndex == -1) {
            throw new ColumnNotFoundException(columnName);
        }
        ColumnModel columnModel = state.getColumnModel();
        ColumnModel newColumnModel = columnModel;
        // set judgment id on recon if changed
        Recon finalRecon = newRecon == null ? null : newRecon.withJudgmentHistoryEntry(context.getHistoryEntryId());
        if (newReconStats != null) {
            newColumnModel = columnModel.withReconStats(columnIndex, newReconStats);
        }
        return state.mapRows(mapFunction(columnIndex, row, finalRecon), newColumnModel);
    }

    static protected RowMapper mapFunction(int cellIndex, long rowId, Recon newRecon) {
        return new RowMapper() {

            private static final long serialVersionUID = -5983834950609157341L;

            @Override
            public Row call(long currentRowId, Row row) {
                if (rowId == currentRowId) {
                    Cell oldCell = row.getCell(cellIndex);
                    Cell newCell = oldCell == null ? null : new Cell(oldCell.value, newRecon);
                    return row.withCell(cellIndex, newCell);
                } else {
                    return row;
                }
            }

            @Override
            public boolean preservesRecordStructure() {
                return true;
            }
        };
    }

    @Override
    public boolean isImmediate() {
        // this does not correspond to an operation
        return false;
    }

}
