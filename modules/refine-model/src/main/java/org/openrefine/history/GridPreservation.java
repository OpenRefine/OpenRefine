
package org.openrefine.history;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Metadata encoding the properties of a {@link org.openrefine.model.changes.Change}, to characterize how it transforms
 * the grid. This is useful to the UI, to determine if and how it can preserve the scrolling position in the grid.
 */
public enum GridPreservation {
    /**
     * No guarantees are asserted about the transformation.
     */
    @JsonProperty("no-row-preservation")
    NO_ROW_PRESERVATION,

    /**
     * The project is guaranteed to have as many rows before and after the change.
     */
    @JsonProperty("preserves-rows")
    PRESERVES_ROWS,

    /**
     * Stronger than {@link #PRESERVES_ROWS}: not only it preserves rows, but the record boundaries stay the same.
     */
    @JsonProperty("preserves-records")
    PRESERVES_RECORDS,
}
