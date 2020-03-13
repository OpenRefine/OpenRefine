package org.openrefine.history.dag;

import org.openrefine.model.ColumnModel;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A slice in the directed acyclic graph of 
 * column dependencies, corresponding to the application
 * of a single operation.
 * 
 * @author Antonin Delpeuch
 *
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, 
        include = JsonTypeInfo.As.PROPERTY, 
        property = "type")
@JsonSubTypes({ 
        @Type(value = AdditionSlice.class, name = "addition"), 
        @Type(value = TransformationSlice.class, name = "transformation"),
        @Type(value = ReorderSlice.class, name = "reorder"),
        @Type(value = OpaqueSlice.class, name = "opaque")
})
public interface DagSlice {

    /**
     * Given the list of columns before the change, return the list 
     * of columns after the slice.
     * 
     * @param columns
     *     the column metadata before the change, in project order
     * @return
     *     the column metadata after the change
     */
    public ColumnModel applyToColumns(ColumnModel columns) throws IncompatibleSliceException;
}
