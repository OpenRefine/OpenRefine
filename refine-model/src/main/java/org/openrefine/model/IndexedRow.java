
package org.openrefine.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A row with its row index. Equivalent to {@code Tuple2<long, Row>} but serializable and deserializable with Jackson
 * easily. Serialization keys are kept short to reduce the memory overhead.
 * 
 * @author Antonin Delpeuch
 */
public class IndexedRow implements Serializable {

    private static final long serialVersionUID = -8959175171376953548L;

    private final long _id;
    private final Row _row;

    @JsonCreator
    public IndexedRow(
            @JsonProperty("i") long id,
            @JsonProperty("r") Row row) {
        _id = id;
        _row = row;
    }

    @JsonProperty("i")
    public long getIndex() {
        return _id;
    }

    @JsonProperty("r")
    public Row getRow() {
        return _row;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof IndexedRow)) {
            return false;
        }
        IndexedRow otherRow = (IndexedRow) other;
        return _id == otherRow._id && _row.equals(otherRow._row);
    }

    @Override
    public int hashCode() {
        return (int) (_id % Integer.MAX_VALUE) + _row.hashCode();
    }

    @Override
    public String toString() {
        return String.format("IndexedRow %d: %s", _id, _row);
    }
}
