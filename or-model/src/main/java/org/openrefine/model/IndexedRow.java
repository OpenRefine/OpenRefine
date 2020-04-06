
package org.openrefine.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import scala.Tuple2;

/**
 * A row with its row index. Equivalent to Tuple2<long, Row> but serializable and deserializable with Jackson easily.
 * Serialization keys are kept short to reduce the memory overhead.
 * 
 * @author Antonin Delpeuch
 */
public class IndexedRow {

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

    @JsonIgnore
    public Tuple2<Long, Row> toTuple() {
        return new Tuple2<Long, Row>(_id, _row);
    }

    public static IndexedRow fromTuple(Tuple2<Long, Row> tuple) {
        return new IndexedRow(tuple._1, tuple._2);
    }
}
