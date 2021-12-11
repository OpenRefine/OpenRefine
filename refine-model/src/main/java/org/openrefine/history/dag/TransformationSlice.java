package org.openrefine.history.dag;

import java.util.Set;

import org.openrefine.model.ColumnModel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TransformationSlice implements DagSlice {
    
    private final String columnName;
    private final Set<String> inputColumns;
    
    @JsonCreator
    public TransformationSlice(
            @JsonProperty("column")
            String columnName,
            @JsonProperty("inputs")
            Set<String> inputColumns) {
        this.columnName = columnName;
        this.inputColumns = inputColumns;
    }

    @Override
    public ColumnModel applyToColumns(ColumnModel columns) throws IncompatibleSliceException {
        for(String inputColumnName : inputColumns) {
            if (columns.getColumnIndexByName(inputColumnName) == -1) {
                throw new IncompatibleSliceException(this, columns);
            }
        }
        return columns;
    }
    
    /**
     * @return
     *    the name of the column transformed by this operation
     */
    @JsonProperty("column")
    public String getColumnName() {
        return columnName;
    }
     
    /**
     * @return
     *     the set of column names this slice depends on,
     *     which may or may not include the transformed column itself.
     */
    @JsonProperty("inputs")
    public Set<String> getInputColumns() {
        return inputColumns;
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TransformationSlice)) {
            return false;
        }
        TransformationSlice otherSlice = (TransformationSlice) other;
        return (columnName.contentEquals(otherSlice.getColumnName()) &&
                inputColumns.equals(otherSlice.getInputColumns()));
    }
    
    @Override
    public String toString() {
        return String.format("[TransformationSlice on \"%s\"]", columnName);
    }
    
    @Override
    public int hashCode() {
       return columnName.hashCode() + 29 * inputColumns.hashCode(); 
    }
}
