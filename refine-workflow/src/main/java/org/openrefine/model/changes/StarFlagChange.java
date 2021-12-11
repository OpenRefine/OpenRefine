
package org.openrefine.model.changes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;

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
    public GridState apply(GridState projectState, ChangeContext context) throws DoesNotApplyException {
        return projectState.mapRows(mapper(rowId, star, value), projectState.getColumnModel());
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

        };
    }

    @Override
    public boolean isImmediate() {
        // no corresponding operation
        return false;
    }

}
