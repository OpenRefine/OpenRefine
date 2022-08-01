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

package com.google.refine.commands.cell;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.commands.Command;
import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.changes.CellChange;
import com.google.refine.process.QuickHistoryEntryProcess;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;

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
        @JsonProperty("pool")
        @JsonInclude(Include.NON_NULL)
        protected Pool pool;

        protected EditResult(
                String code,
                HistoryEntry historyEntry,
                Cell cell,
                Pool pool) {
            this.code = code;
            this.historyEntry = historyEntry;
            this.cell = cell;
            this.pool = pool;
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

            int rowIndex = Integer.parseInt(request.getParameter("row"));
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

            EditOneCellProcess process = new EditOneCellProcess(
                    project,
                    "Edit single cell",
                    rowIndex,
                    cellIndex,
                    value);

            HistoryEntry historyEntry = project.processManager.queueProcess(process);
            if (historyEntry != null) {
                /*
                 * If the operation has been done, return the new cell's data so the client side can update the cell's
                 * rendering right away.
                 */
                Pool pool = new Pool();
                if (process.newCell != null && process.newCell.recon != null) {
                    pool.pool(process.newCell.recon);
                }
                respondJSON(response, new EditResult("ok", historyEntry, process.newCell, pool));
            } else {
                respond(response, "{ \"code\" : \"pending\" }");
            }
        } catch (Exception e) {
            respondException(response, e);
        }
    }

    protected static class EditOneCellProcess extends QuickHistoryEntryProcess {

        final int rowIndex;
        final int cellIndex;
        final Serializable value;
        Cell newCell;

        EditOneCellProcess(
                Project project,
                String briefDescription,
                int rowIndex,
                int cellIndex,
                Serializable value) {
            super(project, briefDescription);

            this.rowIndex = rowIndex;
            this.cellIndex = cellIndex;
            this.value = value;
        }

        @Override
        protected HistoryEntry createHistoryEntry(long historyEntryID) throws Exception {
            Cell cell = _project.rows.get(rowIndex).getCell(cellIndex);
            Column column = _project.columnModel.getColumnByCellIndex(cellIndex);
            if (column == null) {
                throw new Exception("No such column");
            }

            newCell = new Cell(
                    value,
                    cell != null ? cell.recon : null);

            String description = "Edit single cell on row " + (rowIndex + 1) +
                    ", column " + column.getName();

            Change change = new CellChange(rowIndex, cellIndex, cell, newCell);

            return new HistoryEntry(
                    historyEntryID, _project, description, null, change);
        }
    }
}
