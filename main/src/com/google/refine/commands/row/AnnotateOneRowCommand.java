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

package com.google.refine.commands.row;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Project;
import com.google.refine.model.changes.RowFlagChange;
import com.google.refine.model.changes.RowStarChange;
import com.google.refine.process.QuickHistoryEntryProcess;

public class AnnotateOneRowCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");

        try {
            Project project = getProject(request);

            int rowIndex = Integer.parseInt(request.getParameter("row"));

            String starredString = request.getParameter("starred");
            if (starredString != null) {
                boolean starred = "true".endsWith(starredString);
                String description = (starred ? "Star row " : "Unstar row ") + (rowIndex + 1);

                StarOneRowProcess process = new StarOneRowProcess(
                        project,
                        description,
                        rowIndex,
                        starred);

                performProcessAndRespond(request, response, project, process);
                return;
            }

            String flaggedString = request.getParameter("flagged");
            if (flaggedString != null) {
                boolean flagged = "true".endsWith(flaggedString);
                String description = (flagged ? "Flag row " : "Unflag row ") + (rowIndex + 1);

                FlagOneRowProcess process = new FlagOneRowProcess(
                        project,
                        description,
                        rowIndex,
                        flagged);

                performProcessAndRespond(request, response, project, process);
                return;
            }

            respond(response, "{ \"code\" : \"error\", \"message\" : \"invalid command parameters\" }");

        } catch (Exception e) {
            respondException(response, e);
        }
    }

    protected static class StarOneRowProcess extends QuickHistoryEntryProcess {

        final int rowIndex;
        final boolean starred;

        StarOneRowProcess(
                Project project,
                String briefDescription,
                int rowIndex,
                boolean starred) {
            super(project, briefDescription);

            this.rowIndex = rowIndex;
            this.starred = starred;
        }

        @Override
        protected HistoryEntry createHistoryEntry(long historyEntryID) throws Exception {
            return new HistoryEntry(
                    historyEntryID,
                    _project,
                    (starred ? "Star row " : "Unstar row ") + (rowIndex + 1),
                    null,
                    new RowStarChange(rowIndex, starred));
        }
    }

    protected static class FlagOneRowProcess extends QuickHistoryEntryProcess {

        final int rowIndex;
        final boolean flagged;

        FlagOneRowProcess(
                Project project,
                String briefDescription,
                int rowIndex,
                boolean flagged) {
            super(project, briefDescription);

            this.rowIndex = rowIndex;
            this.flagged = flagged;
        }

        @Override
        protected HistoryEntry createHistoryEntry(long historyEntryID) throws Exception {
            return new HistoryEntry(
                    historyEntryID,
                    _project,
                    (flagged ? "Flag row " : "Unflag row ") + (rowIndex + 1),
                    null,
                    new RowFlagChange(rowIndex, flagged));
        }
    }
}
