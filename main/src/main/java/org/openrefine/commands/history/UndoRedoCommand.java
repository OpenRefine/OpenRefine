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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrefine.commands.Command;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Project;

public class UndoRedoCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        Project project = getProject(request);

        long lastDoneID = -1;
        String lastDoneIDString = request.getParameter("lastDoneID");
        if (lastDoneIDString != null) {
            lastDoneID = Long.parseLong(lastDoneIDString);
        } else {
            String undoIDString = request.getParameter("undoID");
            if (undoIDString != null) {
                long undoID = Long.parseLong(undoIDString);

                lastDoneID = project.getHistory().getPrecedingEntryID(undoID);
            }
        }

        try {
            GridPreservation gridPreservation = project.getHistory().undoRedo(lastDoneID);

            respondJSON(response, new Response(gridPreservation));
        } catch (Exception e) {
            respondException(response, e);
        }
    }

    private static class Response {

        @JsonProperty("gridPreservation")
        final GridPreservation gridPreservation;
        @JsonProperty("code")
        final String code = "ok";

        protected Response(GridPreservation gridPreservation) {
            this.gridPreservation = gridPreservation;
        }
    }
}