
package org.openrefine.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A row with its row index. Equivalent to {@code Tuple2<long, Row>} but serializable and deserializable with Jackson
 * easily. Serialization keys are kept short to reduce the memory overhead.
 * <p>
 * This class can also hold a so-called original row id, which is used when sorting grids. The original row id is the
 * original position of the row before sorting the grid. This is used when the user adds sorting settings in the UI
 * without permanently re-ordering the rows yet.
 * 
 */
public class IndexedRow implements Serializable {

    private static final long serialVersionUID = -8959175171376953548L;

    private final long _id;
    private final long _originalId;
    private final Row _row;

    public IndexedRow(
            long id,
            Row row) {
        _id = id;
        _row = row;
        _originalId = -1;
    }

    @JsonCreator
    public IndexedRow(
            @JsonProperty("i") long id,
            @JsonProperty("o") Long originalId,
            @JsonProperty("r") Row row) {
        _id = id;
        _row = row;
        _originalId = originalId == null ? -1 : originalId;
    }

    /**
     * The position (0-based) of the row in the grid.
     */
    @JsonProperty("i")
    public long getIndex() {
        return _id;
    }

    /**
     * The row
     */
    @JsonProperty("r")
    public Row getRow() {
        return _row;
    }

    /**
     * If this grid was obtained by applying a temporary sorting, this returns the original id of the row in the
     * unsorted grid. Otherwise, this returns null.
     */
    @JsonProperty("o")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long getOriginalIndex() {
        return _originalId == -1 ? null : _originalId;
    }

    /**
     * The original index of this row, or if it is not set, the actual one.
     */
    @JsonIgnore
    public long getLogicalIndex() {
        return _originalId == -1 ? _id : _originalId;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof IndexedRow)) {
            return false;
        }
        IndexedRow otherRow = (IndexedRow) other;
        return _id == otherRow._id && _row.equals(otherRow._row) && _originalId == otherRow._originalId;
    }

    @Override
    public int hashCode() {
        return (int) (_id % Integer.MAX_VALUE) + _row.hashCode();
    }

    @Override
    public String toString() {
        if (_originalId == -1) {
            return String.format("IndexedRow %d: %s", _id, _row);
        } else {
            return String.format("IndexRow (%d -> %d): %s", _originalId, _id, _row);
        }
    }
}
