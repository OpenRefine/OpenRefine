
package org.openrefine.operations.cell;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.history.GridPreservation;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.Operation;
import org.openrefine.operations.exceptions.OperationException;

/**
 * An operation which changes the recon field of a single cell.
 */
public class ReconEditOperation implements Operation {

    private final long row;
    private final String columnName;
    private final Judgment judgment;
    private final String identifierSpace;
    private final String schemaSpace;
    private final ReconCandidate match;
    private final String cellValue; // only needed to generate the description
    private final String description;

    @JsonCreator
    public ReconEditOperation(
            @JsonProperty("rowId") long rowId,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("judgment") Judgment judgment,
            @JsonProperty("identifierSpace") String identifierSpace,
            @JsonProperty("schemaSpace") String schemaSpace,
            @JsonProperty("match") ReconCandidate match,
            @JsonProperty("cellValue") String cellValue) {
        this.row = rowId;
        this.columnName = columnName;
        this.judgment = judgment;
        this.identifierSpace = identifierSpace;
        this.schemaSpace = schemaSpace;
        this.match = match;
        this.cellValue = cellValue;
        String cellDescription = "single cell on row " + (rowId + 1) +
                ", column " + columnName +
                ", containing \"" + cellValue + "\"";
        if (judgment == Judgment.None) {
            description = "Discard recon judgment for " + cellDescription;
        } else if (judgment == Judgment.New) {
            description = "Mark to create new item for " + cellDescription;
        } else if (judgment == Judgment.Matched) {
            description = "Match " + match.name +
                    " (" + match.id + ") to " +
                    cellDescription;
        } else {
            description = "Clear recon data for " + cellDescription;
        }
    }

    @Override
    public ChangeResult apply(Grid state, ChangeContext context) throws OperationException {
        int columnIndex = state.getColumnModel().getRequiredColumnIndex(columnName);
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
            return new ChangeResult(state, GridPreservation.PRESERVES_RECORDS);
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
        Grid newState = state.mapRows(mapFunction(columnIndex, row, newRecon), columnModel);

        return new ChangeResult(
                newState,
                GridPreservation.PRESERVES_RECORDS) {

            @JsonProperty("cell")
            public Cell getCell() {
                try {
                    return newState.getRow(row).getCell(columnIndex);
                } catch (IndexOutOfBoundsException e) {
                    return null;
                }
            }
        };
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
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isReproducible() {
        return false;
    }

    /**
     * The 0-based index of the row to change.
     */
    @JsonProperty("rowId")
    public long getRowId() {
        return row;
    }

    /**
     * The name of the column in which the cell can be found.
     */
    @JsonProperty("columnName")
    public String getColumnName() {
        return columnName;
    }

    /**
     * The judgment to set on the recon.
     */
    @JsonProperty("judgment")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Judgment getJudgment() {
        return judgment;
    }

    /**
     * The identifier space of the recon object.
     */
    @JsonProperty("identifierSpace")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getIdentifierSpace() {
        return identifierSpace;
    }

    /**
     * The schema space of the recon object.
     */
    @JsonProperty("schemaSpace")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getSchemaSpace() {
        return schemaSpace;
    }

    @JsonProperty("match")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ReconCandidate getMatch() {
        return match;
    }

    /**
     * The value of the cell in which this recon change is happening. It is only used to generate the operation
     * description.
     */
    @JsonProperty("cellValue")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCellValue() {
        return cellValue;
    }

}
