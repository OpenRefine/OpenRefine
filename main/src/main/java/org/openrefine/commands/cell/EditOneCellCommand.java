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

package org.openrefine.commands.cell;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.commands.Command;
import org.openrefine.history.Change;
import org.openrefine.history.HistoryEntry;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.openrefine.model.changes.CellChange;
import org.openrefine.process.QuickHistoryEntryProcess;
import org.openrefine.util.ParsingUtilities;

public class EditOneCellCommand extends Command {

    protected static class EditResult {

        @JsonProperty("code")
        protected String code;
        @JsonProperty("historyEntry")
        @JsonInclude(Include.NON_NULL)
        protected HistoryEntry historyEntry;
        @JsonProperty("cell")
        @JsonInclude(Include.NON_NULL)
        protected Cell cell;

        protected EditResult(
                String code,
                HistoryEntry historyEntry,
                Cell cell) {
            this.code = code;
            this.historyEntry = historyEntry;
            this.cell = cell;
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
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");

            Project project = getProject(request);

            long rowIndex = Long.parseLong(request.getParameter("row"));
            int cellIndex = Integer.parseInt(request.getParameter("cell"));

            String type = request.getParameter("type");
            String valueString = request.getParameter("value");
            Serializable value = null;

            if ("number".equals(type)) {
                try {
                    value = Long.parseLong(valueString);
                } catch (NumberFormatException e) {
                    value = Double.parseDouble(valueString);
                }
            } else if ("boolean".equals(type)) {
                value = "true".equalsIgnoreCase(valueString);
            } else if ("date".equals(type)) {
                value = ParsingUtilities.stringToDate(valueString);
            } else {
                value = valueString;
            }

            GridState state = project.getCurrentGridState();
            Cell cell = state.getRow(rowIndex).getCell(cellIndex);
            ColumnMetadata column = state.getColumnModel().getColumns().get(cellIndex);
            if (column == null) {
                throw new Exception("No such column");
            }

            Cell newCell = new Cell(
                    value,
                    cell != null ? cell.recon : null);

            String description = "Edit single cell on row " + (rowIndex + 1) +
                    ", column " + column.getName();

            Change change = new CellChange(rowIndex, cellIndex, newCell);

            QuickHistoryEntryProcess process = new QuickHistoryEntryProcess(
                    project.getHistory(),
                    description,
                    null,
                    change);

            HistoryEntry historyEntry = project.getProcessManager().queueProcess(process);
            if (historyEntry != null) {
                /*
                 * If the operation has been done, return the new cell's data so the client side can update the cell's
                 * rendering right away.
                 */
                respondJSON(response, new EditResult("ok", historyEntry, newCell));
            } else {
                respond(response, "{ \"code\" : \"pending\" }");
            }
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
