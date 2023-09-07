
package org.openrefine.operations.row;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.history.GridPreservation;
import org.openrefine.model.Grid;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.Operation;
import org.openrefine.operations.exceptions.OperationException;

/**
 * An operation which updates the flag or star field of a single row in the project.
 */
public class AnnotateOneRowOperation implements Operation {

    @JsonProperty("rowId")
    protected final long rowId;
    @JsonProperty("star")
    protected final boolean star;
    @JsonProperty("value")
    protected final boolean value;

    @JsonCreator
    public AnnotateOneRowOperation(
            @JsonProperty("rowId") long rowId,
            @JsonProperty("star") boolean star,
            @JsonProperty("value") boolean value) {
        this.rowId = rowId;
        this.star = star;
        this.value = value;
    }

    @Override
    public ChangeResult apply(Grid projectState, ChangeContext context) throws OperationException {
        return new ChangeResult(
                projectState.mapRows(mapper(rowId, star, value), projectState.getColumnModel()),
                GridPreservation.PRESERVES_RECORDS);
    }

    @Override
    public String getDescription() {
        if (star) {
            return (value ? "Star row " : "Unstar row ") + (rowId + 1);
        } else {
            return (value ? "Flag row " : "Unflag row ") + (rowId + 1);
        }
    }

    protected static RowMapper mapper(long rowId, boolean star, boolean value) {
        return new RowMapper() {

            private static final long serialVersionUID = -1902866395188130227L;

            @Override
            public Row call(long currentRowId, Row row) {
                if (currentRowId != rowId) {
                    return row;
                }
                if (star) {
                    return row.withStarred(value);
                } else {
                    return row.withFlagged(value);
                }
            }

            @Override
            public boolean preservesRecordStructure() {
                return true;
            }

        };
    }

}
