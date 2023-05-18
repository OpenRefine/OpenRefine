
package org.openrefine.operations.cell;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.expr.ParsingException;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.ReconCellChange;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.operations.Operation;

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
    public Change createChange() throws ParsingException {
        return new ReconCellChange(row, columnName, judgment, identifierSpace, schemaSpace, match);
    }

    @Override
    public String getDescription() {
        return description;
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
