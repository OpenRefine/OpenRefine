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

package com.google.refine.commands.recon;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.commands.Command;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Recon.Judgment;
import com.google.refine.model.ReconStats;
import com.google.refine.model.changes.CellChange;
import com.google.refine.model.changes.ReconChange;
import com.google.refine.process.QuickHistoryEntryProcess;
import com.google.refine.util.Pool;

public class ReconClearOneCellCommand extends Command {

    protected static class CellResponse {

        @JsonProperty("code")
        protected String code = "ok";
        @JsonProperty("historyEntry")
        protected HistoryEntry entry;
        @JsonProperty("cell")
        Cell cell;
        @JsonProperty("pool")
        Pool pool;

        protected CellResponse(HistoryEntry historyEntry, Cell newCell, Pool newPool) {
            entry = historyEntry;
            cell = newCell;
            pool = newPool;
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

            int rowIndex = Integer.parseInt(request.getParameter("row"));
            int cellIndex = Integer.parseInt(request.getParameter("cell"));

            ClearOneCellProcess process = new ClearOneCellProcess(
                    project,
                    "Clear one cell's recon data",
                    rowIndex,
                    cellIndex);

            HistoryEntry historyEntry = project.processManager.queueProcess(process);
            if (historyEntry != null) {
                /*
                 * If the process is done, write back the cell's data so that the client side can update its UI right
                 * away.
                 */
                Pool pool = new Pool();

                if (process.newCell != null && process.newCell.recon != null) {
                    pool.pool(process.newCell.recon);
                }

                respondJSON(response, new CellResponse(historyEntry, process.newCell, pool));
            } else {
                respond(response, "{ \"code\" : \"pending\" }");
            }
        } catch (Exception e) {
            respondException(response, e);
        }
    }

    protected static class ClearOneCellProcess extends QuickHistoryEntryProcess {

        final int rowIndex;
        final int cellIndex;
        Cell newCell;

        ClearOneCellProcess(
                Project project,
                String briefDescription,
                int rowIndex,
                int cellIndex) {
            super(project, briefDescription);

            this.rowIndex = rowIndex;
            this.cellIndex = cellIndex;
        }

        @Override
        protected HistoryEntry createHistoryEntry(long historyEntryID) throws Exception {
            Cell cell = _project.rows.get(rowIndex).getCell(cellIndex);
            if (cell == null || !ExpressionUtils.isNonBlankData(cell.value)) {
                throw new Exception("Cell is blank or error");
            }

            Column column = _project.columnModel.getColumnByCellIndex(cellIndex);
            if (column == null) {
                throw new Exception("No such column");
            }

            Judgment oldJudgment = cell.recon == null ? Judgment.None : cell.recon.judgment;

            newCell = new Cell(cell.value, null);

            ReconStats stats = column.getReconStats();
            if (stats == null) {
                stats = ReconStats.create(_project, cellIndex);
            } else {
                int newChange = 0;
                int matchChange = 0;

                if (oldJudgment == Judgment.New) {
                    newChange--;
                }
                if (oldJudgment == Judgment.Matched) {
                    matchChange--;
                }

                stats = new ReconStats(
                        stats.nonBlanks + 1,
                        stats.newTopics + newChange,
                        stats.matchedTopics + matchChange);
            }

            String description = "Clear recon data for single cell on row " + (rowIndex + 1) +
                    ", column " + column.getName() +
                    ", containing \"" + cell.value + "\"";

            Change change = new ReconChange(
                    new CellChange(rowIndex, cellIndex, cell, newCell),
                    column.getName(),
                    column.getReconConfig(),
                    stats);

            return new HistoryEntry(
                    historyEntryID, _project, description, null, change);
        }
    }
}
