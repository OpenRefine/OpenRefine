package org.openrefine.model.changes;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Helper class to represent an entry in the map,
 * while controlling serialization in JSON.
 * 
 * @author Antonin Delpeuch
 *
 * @param <T>
 */
public class IndexedData<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 6928586351690626940L;
    
    private final long rowId;
    private final T data;
    
    @JsonCreator
    public IndexedData(
            @JsonProperty("i")
            long rowId,
            @JsonProperty("d")
            T data) {
        this.rowId = rowId;
        this.data = data;
    }
    
    @JsonProperty("i")
    public long getId() {
        return rowId;
    }
    
    @JsonProperty("d")
    public T getData() {
        return data;
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof IndexedData)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        IndexedData<T> otherData = (IndexedData<T>)other;
        return rowId == otherData.getId() && data.equals(otherData.getData());
    }
    
    @Override
    public int hashCode() {
        return (int)rowId;
    }
    
    @Override
    public String toString() {
        return String.format("[IndexedData %d %s]", rowId, data);
    }
}