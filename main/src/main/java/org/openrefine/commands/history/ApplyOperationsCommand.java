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

package org.openrefine.commands.history;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.openrefine.commands.Command;
import org.openrefine.history.HistoryEntry;
import org.openrefine.model.Project;
import org.openrefine.model.changes.Change;
import org.openrefine.operations.Operation;
import org.openrefine.operations.UnknownOperation;
import org.openrefine.process.Process;
import org.openrefine.process.QuickHistoryEntryProcess;
import org.openrefine.util.ParsingUtilities;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ApplyOperationsCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        Project project = getProject(request);
        String jsonString = request.getParameter("operations");
        List<OperationApplicationResult> results = new ArrayList<>();
        try {

            ArrayNode a = ParsingUtilities.evaluateJsonStringToArrayNode(jsonString);
            int count = a.size();
            for (int i = 0; i < count; i++) {
                if (a.get(i) instanceof ObjectNode) {
                    ObjectNode obj = (ObjectNode) a.get(i);

                    OperationApplicationResult applicationResult = applyOperation(project, obj);
                    results.add(applicationResult);
                    if ("failed".equals(applicationResult.status)) {
                        respondJSON(response, new JsonResponse("error", results));
                    }
                }
            }

            String code = "ok";
            if (project.getProcessManager().hasPending()) {
                code = "pending";
            }
            respondJSON(response, new JsonResponse(code, results));
        } catch (IOException e) {
            respondException(response, e);
        }
    }

    protected OperationApplicationResult applyOperation(Project project, JsonNode operationJson) {
        Operation operation = null;
        try {
            operation = ParsingUtilities.mapper.convertValue(operationJson, Operation.class);
        } catch (IllegalArgumentException e) {
            return new OperationApplicationResult(e.getMessage());
        }

        if (operation != null && !(operation instanceof UnknownOperation)) {
            try {
                Process process = operation.createProcess(project);
                OperationApplicationResult applicationResult = null;
                if (process.isImmediate() && !project.getProcessManager().hasPending()) {
                    HistoryEntry historyEntry = process.performImmediate();
                    applicationResult = new OperationApplicationResult(historyEntry);
                } else {
                    project.getProcessManager().queueProcess(process);
                    applicationResult = new OperationApplicationResult();
                }
                return applicationResult;
            } catch (Exception e) {
                // TODO make catch block narrower, only catching certain expected exceptions
                // such as DoesNotApplyException. This requires tightening the exceptions in the Process
                // interface too.
                return new OperationApplicationResult(e.getMessage());
            }
        } else {
            return new OperationApplicationResult("Operation could not be parsed");
        }
    }

    protected static class OperationApplicationResult {

        @JsonProperty("status")
        final String status; // whether the operation is "applied", "pending" or "failed"
        @JsonProperty("historyEntry")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        HistoryEntry historyEntry = null; // if the operation is "applied", the corresponding history entry
        @JsonProperty("errorMessage")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String errorMessage = null; // any error message if applying the operation failed
        // TODO let operations expose structured information when they fail

        public OperationApplicationResult(HistoryEntry historyEntry) {
            this.status = "applied";
            this.historyEntry = historyEntry;
        }

        public OperationApplicationResult() {
            this.status = "pending";
        }

        public OperationApplicationResult(String errorMessage) {
            this.status = "failed";
            this.errorMessage = errorMessage;
        }
    }

    protected static class JsonResponse {

        @JsonProperty("code")
        String code; // whether all operation applications were successful
        @JsonProperty("results")
        List<OperationApplicationResult> results; // list of individual results for each operation

        public JsonResponse(String code, List<OperationApplicationResult> results) {
            this.code = code;
            this.results = results;
        }
    }
}
