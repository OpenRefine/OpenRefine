/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.commands.history;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import com.google.refine.commands.Command;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.Recipe;
import com.google.refine.operations.Recipe.RecipeValidationException;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;

public class ApplyOperationsCommand extends Command {

    /**
     * Response for the case when all operations are immediate, returning the corresponding history entries.
     */
    protected static class HistoryEntriesResponse {

        @JsonProperty("code")
        protected String getCode() {
            return "ok";
        }

        @JsonProperty("historyEntries")
        protected List<HistoryEntry> historyEntries;

        protected HistoryEntriesResponse(List<HistoryEntry> entries) {
            historyEntries = entries;
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }
        try {
            Project project = getProject(request);
            String jsonString = request.getParameter("operations");

            Map<String, String> renames = Map.of();
            String renamesJson = request.getParameter("renames");
            if (renamesJson != null) {
                renames = ParsingUtilities.mapper.readValue(renamesJson, new TypeReference<Map<String, String>>() {
                });
            }

            Recipe recipe = ParsingUtilities.mapper.readValue(jsonString, Recipe.class);
            recipe.validate();
            if (!renames.isEmpty()) {
                recipe = recipe.renameColumns(renames);
            }

            // check all required columns are present
            Set<String> requiredColumns = recipe.getRequiredColumns();
            for (String columnName : requiredColumns) {
                if (project.columnModel.getColumnByName(columnName) == null) {
                    throw new IllegalArgumentException(
                            "Column '" + columnName + "' is referenced in the list of operations but is absent from the project");
                }
            }

            // Run all operations in sequence
            List<HistoryEntry> entries = new ArrayList<>(recipe.getOperations().size());
            for (AbstractOperation operation : recipe.getOperations()) {
                Process process = operation.createProcess(project, new Properties());
                HistoryEntry entry = project.processManager.queueProcess(process);
                if (entry != null) {
                    entries.add(entry);
                }
            }

            if (project.processManager.hasPending()) {
                respond(response, "{ \"code\" : \"pending\" }");
            } else {
                respondJSON(response, new HistoryEntriesResponse(entries));
            }
        } catch (RecipeValidationException e) {
            respondJSON(response, e);
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
