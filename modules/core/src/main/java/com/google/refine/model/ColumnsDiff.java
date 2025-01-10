
package com.google.refine.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.Validate;

/**
 * Represents the changes made by an operation to the set of columns in a project.
 */
public class ColumnsDiff {

    private final List<AddedColumn> addedColumns;
    private final Set<String> deletedColumns;
    private final Set<String> modifiedColumns;

    private final static ColumnsDiff empty = new ColumnsDiff(List.of(), Set.of(), Set.of());

    /**
     * An empty diff, for when columns don't change at all.
     */
    public static ColumnsDiff empty() {
        return empty;
    }

    /**
     * A column diff which for an operation that only modifies a single column
     */
    public static ColumnsDiff modifySingleColumn(String name) {
        return builder().modifyColumn(name).build();
    }

    /**
     * A fresh builder object.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Constructor. Consider using {@link Builder} instead.
     * 
     * @param addedColumns
     *            the list of column names that appear after this operation, in the order they appear in the new table
     * @param deletedColumns
     *            the set of column names that disappear after this operation
     * @param modifiedColumns
     *            the names of existing columns whose contents are modified by this operation
     */
    public ColumnsDiff(List<AddedColumn> addedColumns, Set<String> deletedColumns, Set<String> modifiedColumns) {
        this.addedColumns = addedColumns;
        this.deletedColumns = deletedColumns;
        this.modifiedColumns = modifiedColumns;
    }

    /**
     * The columns names that were absent before and present after the operation. This includes the new names of any
     * renamed columns.
     */
    @JsonProperty("added")
    public List<AddedColumn> getAddedColumns() {
        return addedColumns;
    }

    /**
     * Convenience method to return only added column names, without position info.
     */
    @JsonIgnore
    public List<String> getAddedColumnNames() {
        return addedColumns.stream().map(AddedColumn::getName).collect(Collectors.toList());
    }

    /**
     * The columns names that were present before and absent after the operation. This includes the old names of any
     * renamed columns.
     */
    @JsonProperty("deleted")
    public Set<String> getDeletedColumns() {
        return deletedColumns;
    }

    /**
     * The names of columns that are modified by this operation.
     */
    @JsonProperty("modified")
    public Set<String> getModifiedColumns() {
        return modifiedColumns;
    }

    /**
     * The set of columns that are implied to be present in the original project
     * 
     * @return
     */
    @JsonProperty("impliedDependencies")
    public Set<String> getImpliedDependencies() {
        Set<String> dependencies = new HashSet<>();
        dependencies.addAll(getDeletedColumns());
        dependencies.addAll(getModifiedColumns());
        List<String> allAddedColumnNames = getAddedColumnNames();
        for (AddedColumn addedColumn : getAddedColumns()) {
            if (addedColumn.getAfterName() != null && !allAddedColumnNames.contains(addedColumn.getAfterName())) {
                dependencies.add(addedColumn.getAfterName());
            }
        }
        return dependencies;
    }

    public static class Builder {

        private final List<AddedColumn> added = new ArrayList<>();
        private final Set<String> deleted = new HashSet<>();
        private final Set<String> modified = new HashSet<>();
        private boolean built = false;

        public Builder addColumn(AddedColumn columnName) {
            Validate.isTrue(!built, "The ColumnsDiff was already built");
            added.add(columnName);
            return this;
        }

        public Builder addColumn(String name, String after) {
            Validate.isTrue(!built, "The ColumnsDiff was already built");
            added.add(new AddedColumn(name, after));
            return this;
        }

        public Builder deleteColumn(String columnName) {
            Validate.isTrue(!built, "The ColumnsDiff was already built");
            deleted.add(columnName);
            return this;
        }

        public Builder modifyColumn(String columnName) {
            Validate.isTrue(!built, "The ColumnsDiff was already built");
            modified.add(columnName);
            return this;
        }

        public ColumnsDiff build() {
            Validate.isTrue(!built, "The ColumnsDiff was already built");
            built = true;
            return new ColumnsDiff(added, deleted, modified);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(addedColumns, deletedColumns, modifiedColumns);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ColumnsDiff other = (ColumnsDiff) obj;
        return Objects.equals(addedColumns, other.addedColumns) && Objects.equals(deletedColumns, other.deletedColumns)
                && Objects.equals(modifiedColumns, other.modifiedColumns);
    }

    @Override
    public String toString() {
        return "ColumnsDiff [addedColumns=" + addedColumns + ", deletedColumns=" + deletedColumns + ", modifiedColumns=" + modifiedColumns
                + "]";
    }

}
