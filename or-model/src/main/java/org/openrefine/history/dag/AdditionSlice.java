package org.openrefine.history.dag;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * History slice which represents the addition of multiple consecutive columns,
 * which all depend on the same set of input columns.
 * 
 * @author Antonin Delpeuch
 *
 */
public class AdditionSlice implements DagSlice {
    
    private final Set<String> inputColumns;
    private final List<ColumnMetadata> outputColumns;
    private final int insertionPosition;
    
    @JsonCreator
    public AdditionSlice(
            @JsonProperty("inputs")
            Set<String> inputColumns,
            @JsonProperty("outputs")
            List<ColumnMetadata> outputColumns,
            @JsonProperty("position")
            int insertionPosition) {
        this.inputColumns = inputColumns;
        this.outputColumns = outputColumns;
        this.insertionPosition = insertionPosition;
    }

    @Override
    public ColumnModel applyToColumns(ColumnModel columns) throws IncompatibleSliceException {
        for(String inputColumnName : inputColumns) {
            if (columns.getColumnIndexByName(inputColumnName) == -1) {
                throw new IncompatibleSliceException(this, columns);
            }
        }
        List<ColumnMetadata> newColumns = new LinkedList<>(columns.getColumns());
        newColumns.addAll(insertionPosition, outputColumns);
        return new ColumnModel(newColumns);
    }
     
    /**
     * @return
     *     the set of column names this slice depends on
     */
    @JsonProperty("inputs")
    public Set<String> getInputColumns() {
        return inputColumns;
    }
    
    /**
     * @return
     *    the list of columns created by the operation,
     *    in the insertion order
     */
    @JsonProperty("outputs")
    public List<ColumnMetadata> getOutputColumns() {
        return outputColumns;
    }
    
    /**
     * @return
     *     the position in the list of columns where new columns
     *     are inserted
     */
    @JsonProperty("position")
    public int getInsertionPosition() {
        return insertionPosition;
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AdditionSlice)) {
            return false;
        }
        AdditionSlice otherSlice = (AdditionSlice) other;
        return (
                inputColumns.equals(otherSlice.getInputColumns()) &&
                outputColumns.equals(otherSlice.getOutputColumns()) &&
                insertionPosition == otherSlice.getInsertionPosition()
                );
    }
    
    @Override
    public String toString() {
        return String.format("[AdditionSlice for %s]",
                String.join(", ", outputColumns.stream().map(s -> "\"" + s + "\"").collect(Collectors.toList())));
    }
    
    @Override
    public int hashCode() {
        return inputColumns.hashCode() + 17 * outputColumns.hashCode() + 29 * insertionPosition;
    }

}
