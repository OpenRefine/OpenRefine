package org.openrefine.history.dag;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A slice where columns can be reordered (including removing columns)
 * and renamed in one go.
 * 
 * @author Antonin Delpeuch
 */
public class ReorderSlice implements DagSlice {
    
    private final List<String> reorderedColumns;
    private final Map<String, String> renames;
    
    /**
     * Constructs a reordering slice.
     * 
     * @param reorderedColumns
     *     the list of column names after reordering, but before renaming
     * @param renames
     *     the column renames, as a map from old names to new names
     */
    @JsonCreator
    public ReorderSlice(
            @JsonProperty("reorderedColumns")
            List<String> reorderedColumns,
            @JsonProperty("renames")
            Map<String, String> renames) {
        this.reorderedColumns = reorderedColumns;
        this.renames = renames != null ? renames : Collections.emptyMap();
    }
    

    @Override
    public ColumnModel applyToColumns(ColumnModel columns) throws IncompatibleSliceException {
        List<ColumnMetadata> newColumns = new LinkedList<>();
        for(String columnName : reorderedColumns) {
            int idx = columns.getColumnIndexByName(columnName);
            if (idx == -1) {
                throw new IncompatibleSliceException(this, columns);
            }
            ColumnMetadata column = columns.getColumns().get(idx);
            String newName = renames.getOrDefault(column.getName(), column.getName());
            newColumns.add(column.withName(newName));
        }
        return new ColumnModel(newColumns);
    }

    /**
     * @return
     *    the list of column names after reordering, but before renaming
     */
    @JsonProperty("reorderedColumns")
    public List<String> getReorderedColumns() {
        return reorderedColumns;
    }

    /**
     * @return
     *     the column renames, as a map from old names to new names
     */
    @JsonProperty("renames")
    public Map<String, String> getRenames() {
        return renames;
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ReorderSlice)) {
            return false;
        }
        ReorderSlice otherSlice = (ReorderSlice)other;
        return (renames.equals(otherSlice.getRenames()) &&
                reorderedColumns.equals(otherSlice.getReorderedColumns()));
    }
    
    @Override
    public String toString() {
        return String.format("[ReorderSlice with final columns %s]",
                String.join(", ", reorderedColumns.stream().map(s -> "\"" + renames.getOrDefault(s, s) + "\"").collect(Collectors.toList())));
    }
    
    @Override
    public int hashCode() {
       return reorderedColumns.hashCode() + 43 * renames.hashCode(); 
    }

}
