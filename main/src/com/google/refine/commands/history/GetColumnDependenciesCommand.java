
package com.google.refine.commands.history;

import java.io.IOException;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.commands.Command;
import com.google.refine.operations.Recipe;
import com.google.refine.util.ParsingUtilities;

/**
 * Computes the column dependencies of a list of operations, provided in the same JSON format as to the
 * {@link ApplyOperationsCommand}.
 */
public class GetColumnDependenciesCommand extends Command {

    class Result {

        @JsonProperty("code")
        String code = "ok";
        @JsonProperty("dependencies")
        Set<String> dependencies;
        @JsonProperty("newColumns")
        Set<String> newColumns;
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

            respondJSON(response, result);
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
