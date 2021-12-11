package org.openrefine.history.dag;

import org.openrefine.model.ColumnModel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An opaque transformation step, which does not commute with any other step.
 * It may use the records mode, or not be row-wise at all.
 * 
 * @author Antonin Delpeuch
 *
 */
public class OpaqueSlice implements DagSlice {
    
    public final ColumnModel columnModel;

    /**
     * Creates an opaque slice.
     * 
     * @param columnModel
     *     the state of the column model after execution of the slice.
     */
    @JsonCreator
    public OpaqueSlice(
            @JsonProperty("columnModel")
            ColumnModel columnModel) {
        this.columnModel = columnModel;
    }

    @Override
    public ColumnModel applyToColumns(ColumnModel columns) throws IncompatibleSliceException {
        return columnModel;
    }

    /**
     * @return columnModel
     *     the state of the column model after execution of the slice
     */
    @JsonProperty("columnModel")
    public ColumnModel getColumnModel() {
        return columnModel;
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof OpaqueSlice)) {
            return false;
        }
        OpaqueSlice otherSlice = (OpaqueSlice) other;
        return columnModel.equals(otherSlice.getColumnModel());
    }
    
    @Override
    public int hashCode() {
        return columnModel.hashCode();
    }
    
    @Override
    public String toString() {
        return "[OpaqueSlice]";
    }

}
