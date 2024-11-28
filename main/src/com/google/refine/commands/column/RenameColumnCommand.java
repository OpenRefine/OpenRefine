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

package com.google.refine.commands.column;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.browsing.EngineConfig;
import com.google.refine.commands.Command;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.column.ColumnRenameOperation;
import com.google.refine.process.Process;

public class RenameColumnCommand extends Command {

    protected static class RenameResult extends HistoryEntryResponse {

        protected RenameResult(HistoryEntry entry) {
            super(entry);
        }

        @JsonProperty("newEngineConfig")
        @JsonInclude(Include.NON_NULL)
        EngineConfig newEngineConfig = null;

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

            String oldColumnName = request.getParameter("oldColumnName");
            String newColumnName = request.getParameter("newColumnName");

            AbstractOperation op = new ColumnRenameOperation(oldColumnName, newColumnName);
            op.validate();
            Process process = op.createProcess(project, new Properties());

            HistoryEntry historyEntry = project.processManager.queueProcess(process);
            if (historyEntry != null) {
                RenameResult result = new RenameResult(historyEntry);

                // also translate the facets if they have been provided
                EngineConfig config = getEngineConfig(request);
                if (config != null) {
                    Map<String, String> rename = Map.of(oldColumnName, newColumnName);
                    result.newEngineConfig = config.renameColumnDependencies(rename);
                }

                respondJSON(response, result);
            } else {
                respondCodePending(response);
            }
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
