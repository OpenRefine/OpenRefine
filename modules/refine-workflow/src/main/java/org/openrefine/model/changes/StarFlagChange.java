
package org.openrefine.model.changes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.history.GridPreservation;
import org.openrefine.model.Grid;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;

/**
 * A change which updates the flag or star field of a single row in the project.
 */
public class StarFlagChange implements Change {

    @JsonProperty("rowId")
    protected final long rowId;
    @JsonProperty("star")
    protected final boolean star;
    @JsonProperty("value")
    protected final boolean value;

    @JsonCreator
    public StarFlagChange(
            @JsonProperty("rowId") long rowId,
            @JsonProperty("star") boolean star,
            @JsonProperty("value") boolean value) {
        this.rowId = rowId;
        this.star = star;
        this.value = value;
    }

    @Override
    public ChangeResult apply(Grid projectState, ChangeContext context) throws DoesNotApplyException {
        return new ChangeResult(
                projectState.mapRows(mapper(rowId, star, value), projectState.getColumnModel()),
                GridPreservation.PRESERVES_RECORDS);
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

    @Override
    public boolean isImmediate() {
        // no corresponding operation
        return false;
    }

}
