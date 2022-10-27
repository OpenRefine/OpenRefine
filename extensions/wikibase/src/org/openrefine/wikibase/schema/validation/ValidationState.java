
package org.openrefine.wikibase.schema.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.refine.model.ColumnModel;

/**
 * A sort of logger which can be used when traversing a schema and logging issues about it. By using the
 * {@link #enter()} and {@link #leave()} methods appropriately, it keeps track of the path taken to arrive at the
 * current position in the schema.
 * 
 * @author Antonin Delpeuch
 *
 */
public class ValidationState {

    private final List<Optional<PathElement>> currentPath;
    private final List<ValidationError> validationErrors;
    private final ColumnModel columnModel;

    public ValidationState(ColumnModel columnModel) {
        currentPath = new ArrayList<>();
        validationErrors = new ArrayList<>();
        this.columnModel = columnModel;
    }

    /**
     * Called when entering a schema element which should not be recorded in the path displayed to the user.
     * 
     * Useful for classes like StatementGroup or SnakGroup which do not really have a user-facing counterpart.
     */
    public ValidationState enter() {
        currentPath.add(Optional.empty());
        return this;
    }

    /**
     * Called when entering a schema element which should be recorded in the path displayed to the user.
     */
    public ValidationState enter(PathElement element) {
        currentPath.add(Optional.of(element));
        return this;
    }

    /**
     * Called when leaving a schema element and returning to its parent.
     */
    public ValidationState leave() {
        if (currentPath.isEmpty()) {
            throw new IllegalStateException("Already at the root level of the schema");
        }
        currentPath.remove(currentPath.size() - 1);
        return this;
    }

    /**
     * Logs an error at the current position in the schema
     * 
     * @param message
     *            the error message
     */
    public ValidationState addError(String message) {
        List<PathElement> cleanedPath = currentPath.stream()
                .filter(element -> element.isPresent())
                .map(element -> element.get())
                .collect(Collectors.toList());
        validationErrors.add(new ValidationError(cleanedPath, message));
        return this;
    }

    /**
     * Returns the column model in the context of which this schema is validated.
     */
    public ColumnModel getColumnModel() {
        return columnModel;
    }

    /**
     * Retrieves the validation errors emitted so far.
     */
    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

}
