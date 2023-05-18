
package org.openrefine.model.changes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.history.GridPreservation;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;

/**
 * A change which updates the recon field of a single cell in the project.
 */
public class ReconCellChange implements Change {

    @JsonProperty("rowId")
    protected final long row;
    @JsonProperty("columnName")
    protected final String columnName;
    @JsonProperty("judgment")
    protected final Judgment judgment;
    @JsonProperty("identifierSpace")
    protected final String identifierSpace;
    @JsonProperty("schemaSpace")
    protected final String schemaSpace;
    @JsonProperty("match")
    protected final ReconCandidate match;

    @JsonCreator
    public ReconCellChange(
            @JsonProperty("rowId") long rowId,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("judgment") Judgment judgment,
            @JsonProperty("identifierSpace") String identifierSpace,
            @JsonProperty("schemaSpace") String schemaSpace,
            @JsonProperty("match") ReconCandidate match) {
        this.row = rowId;
        this.columnName = columnName;
        this.judgment = judgment;
        this.identifierSpace = identifierSpace;
        this.schemaSpace = schemaSpace;
        this.match = match;
    }

    @Override
    public ChangeResult apply(Grid state, ChangeContext context) throws DoesNotApplyException {
        int columnIndex = state.getColumnModel().getColumnIndexByName(columnName);
        if (columnIndex == -1) {
            throw new ColumnNotFoundException(columnName);
        }
        ColumnModel columnModel = state.getColumnModel();
        ColumnMetadata column = columnModel.getColumnByIndex(columnIndex);

        Cell cell = null;
        try {
            Row row = state.getRow(this.row);
            cell = row.getCell(columnIndex);
        } catch (IndexOutOfBoundsException e) {
        }
        if (cell == null) {
            // leave the grid unchanged
            return new ChangeResult(state, GridPreservation.PRESERVES_RECORDS, null);
        }
        Recon newRecon = null;
        if (cell.recon != null) {
            newRecon = cell.recon.withJudgmentHistoryEntry(context.getHistoryEntryId());
        } else if (identifierSpace != null && schemaSpace != null) {
            newRecon = new Recon(context.getHistoryEntryId(), identifierSpace, schemaSpace);
        } else if (column.getReconConfig() != null) {
            newRecon = column.getReconConfig().createNewRecon(context.getHistoryEntryId());
        } else {
            // This should only happen if we are judging a cell in a column that
            // has never been reconciled before.
            // TODO we should rather throw an exception in this case,
            // ReconConfig should be required on the column.
            newRecon = new Recon(context.getHistoryEntryId(), null, null);
        }
        newRecon = newRecon
                .withMatchRank(-1)
                .withJudgmentAction("single");

        if (judgment == Judgment.None) {
            newRecon = newRecon.withJudgment(Recon.Judgment.None)
                    .withMatch(null);
        } else if (judgment == Judgment.New) {
            newRecon = newRecon
                    .withJudgment(Recon.Judgment.New)
                    .withMatch(null);
        } else if (judgment == Judgment.Matched) {
            newRecon = newRecon.withJudgment(Recon.Judgment.Matched)
                    .withMatch(match);
            if (newRecon.candidates != null) {
                for (int m = 0; m < newRecon.candidates.size(); m++) {
                    if (newRecon.candidates.get(m).id.equals(match.id)) {
                        newRecon = newRecon.withMatchRank(m);
                        break;
                    }
                }
            }
        } else {
            newRecon = null;
        }
        return new ChangeResult(
                state.mapRows(mapFunction(columnIndex, row, newRecon), columnModel),
                GridPreservation.PRESERVES_RECORDS);
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
