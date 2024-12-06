
package com.google.refine.operations;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonValue;

import com.google.refine.model.AbstractOperation;
import com.google.refine.model.ColumnsDiff;

/**
 * A list of operations to be applied in the specified order.
 */
public class Recipe {

    private final List<AbstractOperation> operations;

    @JsonCreator
    public Recipe(
            @JsonUnwrapped List<AbstractOperation> operations) {
        this.operations = operations;
    }

    @JsonValue
    public List<AbstractOperation> getOperations() {
        return operations;
    }

    /**
     * Checks that all operations in this recipe are valid (non null, known operations with valid parameters).
     */
    public void validate() {
        for (AbstractOperation op : operations) {
            if (op == null) {
                throw new IllegalArgumentException("The list of operations contains 'null'");
            }
            if (op instanceof UnknownOperation) {
                throw new IllegalArgumentException("Unknown operation id: " + op.getOperationId());
            }
            op.validate();
        }
    }

    /**
     * Computes which columns are required to be present in the project before applying the list of operations. The set
     * that is returned is an under-approximation: if certain operations in the list fail to analyze their dependencies
     * or their impact on the set of columns, then some required columns will be missed by this method, resulting in an
     * error that will only be detected when the list of operations is applied.
     * 
     * @return a set of required column names
     */
    public Set<String> computeRequiredColumns() {
        // columnNames represents the current set of column names in the project,
        // after having applied the operations scanned so far. If it is empty, then
        // that means we lost track of which columns are present.
        Optional<Set<String>> currentColumnNames = Optional.of(new HashSet<>());
        // keeps track of which columns are required to exist in the project before
        // the first operation is run.
        Set<String> requiredColumnNames = new HashSet<>();

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
                        if (requiredColumnNames.contains(columnName)) {
                            // if this column has already been required before,
                            // but is no longer part of the current columns,
                            // that means it has been deleted since.
                            throw new IllegalArgumentException(
                                    "Inconsistent list of operations: column '" + columnName + "' used after being deleted or renamed");
                        } else {
                            requiredColumnNames.add(columnName);
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
            }
        }
        return requiredColumnNames;
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
}
