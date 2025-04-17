
package com.google.refine.commands.history;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.commands.Command;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.operations.Recipe;
import com.google.refine.operations.Recipe.RecipeValidationException;
import com.google.refine.util.ParsingUtilities;

/**
 * Computes the column dependencies of a list of operations, provided in the same JSON format as to the
 * {@link ApplyOperationsCommand}.
 */
public class GetColumnDependenciesCommand extends Command {

    protected static class Result {

        @JsonProperty("code")
        String code = "ok";
        @JsonProperty("dependencies")
        Set<String> dependencies;
        @JsonProperty("newColumns")
        Set<String> newColumns;
        @JsonProperty("steps")
        List<AnnotatedOperation> steps;
    }

    protected static class AnnotatedOperation {

        @JsonProperty("operation")
        AbstractOperation op;
        @JsonProperty("dependencies")
        Optional<List<String>> dependencies;
        @JsonProperty("columnsDiff")
        Optional<ColumnsDiff> columnsDiff;
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }
        try {
            String jsonString = request.getParameter("operations");

            Recipe recipe = ParsingUtilities.mapper.readValue(jsonString, Recipe.class);

            Result result = new Result();
            result.dependencies = recipe.getRequiredColumns();
            result.newColumns = recipe.getNewColumns();
            result.steps = recipe.getOperations().stream()
                    .map(op -> {
                        var annotated = new AnnotatedOperation();
                        annotated.columnsDiff = op.getColumnsDiff();
                        annotated.dependencies = Optional.empty();
                        Optional<Set<String>> dependencySet = op.getColumnDependencies();
                        if (dependencySet.isPresent()) {
                            annotated.dependencies = Optional.of(dependencySet.get().stream().sorted().collect(Collectors.toList()));
                        }
                        annotated.op = op;
                        return annotated;
                    })
                    .collect(Collectors.toList());

            respondJSON(response, result);
        } catch (RecipeValidationException e) {
            respondJSON(response, e);
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
