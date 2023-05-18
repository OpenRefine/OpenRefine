
package org.openrefine.operations.row;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.expr.ParsingException;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.StarFlagChange;
import org.openrefine.operations.Operation;

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
    public Change createChange() throws ParsingException {
        return new StarFlagChange(rowId, star, value);
    }

    @Override
    public String getDescription() {
        if (star) {
            return (value ? "Star row " : "Unstar row ") + (rowId + 1);
        } else {
            return (value ? "Flag row " : "Unflag row ") + (rowId + 1);
        }
    }

}
