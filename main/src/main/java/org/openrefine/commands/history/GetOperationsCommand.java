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
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.commands.Command;
import org.openrefine.history.HistoryEntry;
import org.openrefine.model.Project;
import org.openrefine.operations.Operation;

public class GetOperationsCommand extends Command {

    protected static class SimpleHistoryEntry {

        protected HistoryEntry entry;

        public SimpleHistoryEntry(HistoryEntry e) {
            entry = e;
        }

        @JsonProperty("description")
        public String getDescription() {
            return entry.getDescription();
        }

        @JsonProperty("operation")
        @JsonInclude(Include.NON_NULL)
        public Operation getOperation() {
            Operation operation = entry.getOperation();
            if (operation.isReproducible()) {
                return entry.getOperation();
            } else {
                return null;
            }
        }
    }

    protected static class HistoryEntries {

        @JsonProperty("entries")
        List<SimpleHistoryEntry> entries;

        protected HistoryEntries(List<HistoryEntry> entries) {
            this.entries = entries.stream()
                    .map(e -> new SimpleHistoryEntry(e))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Project project = getProject(request);

        HistoryEntries entries = new HistoryEntries(project.getHistory().getLastPastEntries(-1));
        respondJSON(response, entries);
    }

}
