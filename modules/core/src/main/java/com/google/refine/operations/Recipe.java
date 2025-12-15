
package com.google.refine.operations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonValue;

import com.google.refine.model.AbstractOperation;
import com.google.refine.model.ColumnsDiff;

/**
 * A list of operations to be applied in the specified order.
 * 
 * @since 3.10
 */
public class Recipe {

    private final List<AbstractOperation> operations;
    private Set<String> dependencies;
    private Set<String> newColumns;
    private Set<String> internalColumns;

    @JsonCreator
    public Recipe(
            @JsonUnwrapped List<AbstractOperation> operations) {
        this.operations = operations;
        this.dependencies = null;
        this.newColumns = null;
        this.internalColumns = null;
    }

    @JsonValue
    public List<AbstractOperation> getOperations() {
        return operations;
    }

    /**
     * Checks that all operations in this recipe are valid (non null, known operations with valid parameters).
     */
    public void validate() {
        int index = 0;
        for (AbstractOperation op : operations) {
            if (op == null) {
                throw new RecipeValidationException(index, "The operation is 'null'");
            }
            if (op instanceof UnknownOperation) {
                throw new RecipeValidationException(index, "Unknown operation " + op.getOperationId());
            }
            try {
                op.validate();
            } catch (IllegalArgumentException e) {
                throw new RecipeValidationException(index, e.getMessage());
            }
        }

        // currentColumnNames represents the current set of column names in the project,
        // after having applied the operations scanned so far. If it is empty, then
        // that means we lost track of which columns are present.
        Optional<Set<String>> currentColumnNames = Optional.of(new HashSet<>());
        // keeps track of which columns are required to exist in the project before
        // the first operation is run.
        dependencies = new HashSet<>();
        // columns created by the recipe
        newColumns = new HashSet<>();
        // columns only created during the recipe but deleted before the end of the recipe
        internalColumns = new HashSet<>();

        for (AbstractOperation op : operations) {
            if (currentColumnNames.isPresent()) {
                Set<String> allDependencies = new HashSet<>();

                Optional<Set<String>> columnDependencies = op.getColumnDependencies();
                if (columnDependencies.isPresent()) {
                    allDependencies.addAll(columnDependencies.get());
                }

                Optional<ColumnsDiff> columnsDiff = op.getColumnsDiff();
                if (columnsDiff.isPresent()) {
                    allDependencies.addAll(columnsDiff.get().getImpliedDependencies());
                }

                for (String columnName : allDependencies) {
                    if (!currentColumnNames.get().contains(columnName)) {
                        if (dependencies.contains(columnName)) {
                            // if this column has already been required before,
                            // but is no longer part of the current columns,
                            // that means it has since been deleted.
                            throw new IllegalArgumentException(
                                    "Inconsistent list of operations: column '" + columnName + "' used after being deleted or renamed");
                        } else {
                            dependencies.add(columnName);
                            currentColumnNames.get().add(columnName);
                        }
                    }
                }
            }

            Optional<ColumnsDiff> columnsDiff = op.getColumnsDiff();
            if (columnsDiff.isEmpty()) {
                currentColumnNames = Optional.empty();
            } else if (currentColumnNames.isPresent()) {
                currentColumnNames.get().removeAll(columnsDiff.get().getDeletedColumns());
                for (String addedColumn : columnsDiff.get().getAddedColumnNames()) {
                    if (currentColumnNames.get().contains(addedColumn)) {
                        throw new IllegalArgumentException(
                                "Creation of column '" + addedColumn + "' conflicts with an existing column with the same name");
                    }
                }
                currentColumnNames.get().addAll(columnsDiff.get().getAddedColumnNames());
                Set<String> newInternalColumns = new HashSet<>(columnsDiff.get().getDeletedColumns());
                newInternalColumns.retainAll(newColumns);
                internalColumns.addAll(newInternalColumns);
                newColumns.removeAll(newInternalColumns);
                newColumns.addAll(columnsDiff.get().getAddedColumnNames());
            }
        }
    }

    /**
     * Computes which columns are required to be present in the project before applying this recipe. The set that is
     * returned is an under-approximation: if certain operations in the list fail to analyze their dependencies or their
     * impact on the set of columns, then some required columns will be missed by this method, resulting in an error
     * that will only be detected when the list of operations is applied.
     * 
     * @return a set of required column names
     */
    public Set<String> getRequiredColumns() {
        if (dependencies == null) {
            validate();
        }
        return dependencies;
    }

    /**
     * Computes which columns will be created by applying this recipe. The set that is returned is an
     * under-approximation: if certain operations in the list fail to expose their impact on the set of columns, then
     * the columns created at this stage will be omitted from the return value of this method.
     * 
     * @return a set of created column names
     */
    public Set<String> getNewColumns() {
        if (newColumns == null) {
            validate();
        }
        return newColumns;
    }

    /**
     * Computes the set of columns created throughout the recipe, but which get deleted before the end of the recipe.
     * This is an under-approximation: if certain operations in the list fail to expose their impac on the set of
     * columns, then the columns created at this stage will be omitted from the return value.
     * 
     * @return a set of internal column names
     */
    public Set<String> getInternalColumns() {
        if (internalColumns == null) {
            validate();
        }
        return internalColumns;
    }

    /**
     * Compute a new version of this recipe, where the column dependencies have been renamed according to the map
     * supplied.
     * 
     * @param newColumnNames
     *            the map from old column names to the new ones
     * @return a new recipe, only if all the operations involved could be successfully renamed
     */
    public Recipe renameColumns(Map<String, String> newColumnNames) {
        List<AbstractOperation> result = operations.stream()
                .map(op -> op.renameColumns(newColumnNames))
                .collect(Collectors.toList());
        return new Recipe(result);
    }

    /**
     * Takes a list of column names and returns a new version of this recipe with all operations
     * that operate on those columns removed
     *
     * @param unusedColumns
     * @return
     */
    public Recipe removeUnnecessaryOperations(Set<String> unusedColumns) {
        List<AbstractOperation> result = operations.stream()
            .filter(o -> !o.dependsOnAny(unusedColumns))
            .collect(Collectors.toList());
        return new Recipe(result);
    }

    /**
     * In preparation for applying the recipe to a project, ensure that the internal column names in the recipe are
     * disjoint from the columns present in the project. The conflicting column names are deduplicated by adding a
     * number at the end of the column such that there is no conflict anymore.
     * 
     * @param projectColumnNames
     *            the set of column names in the project
     * @return a new version of the recipe, where any conflicting internal column names have been deduplicated.
     */
    public Recipe avoidInternalColumnCollisions(Set<String> projectColumnNames) {
        Set<String> allColumns = new HashSet<>(projectColumnNames);
        allColumns.addAll(getNewColumns());

        Set<String> conflictingInternal = new HashSet<>(getInternalColumns());
        conflictingInternal.retainAll(allColumns);
        allColumns.addAll(getInternalColumns());

        Map<String, String> rename = new HashMap<>();
        for (String conflicting : conflictingInternal) {
            int deduplicatingNumber = 1;
            String newName;
            do {
                deduplicatingNumber++;
                newName = String.format("%s_%d", conflicting, deduplicatingNumber);
            } while (allColumns.contains(newName));
            rename.put(conflicting, newName);
        }
        return this.renameColumns(rename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operations);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Recipe other = (Recipe) obj;
        return Objects.equals(operations, other.operations);
    }

    @Override
    public String toString() {
        return "Recipe [operations=" + operations + "]";
    }

    public static class RecipeValidationException extends IllegalArgumentException {

        private static final long serialVersionUID = 196414487572541357L;
        @JsonProperty("operationIndex")
        protected final int index;
        @JsonProperty("operationValidationMessage")
        protected final String operationValidationMessage;
        @JsonProperty("code")
        protected final String code = "error";

        public RecipeValidationException(int index, String message) {
            this.operationValidationMessage = message;
            this.index = index;
        }

        @JsonProperty("message")
        @Override
        public String getMessage() {
            return String.format("Operation #%d: %s", (index + 1), operationValidationMessage);
        }
    }
}
