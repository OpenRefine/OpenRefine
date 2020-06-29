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

package org.openrefine.commands.recon;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.commands.Command;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.history.HistoryEntry;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.ReconCellChange;
import org.openrefine.model.recon.LazyReconStats;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.model.recon.ReconStats;
import org.openrefine.process.QuickHistoryEntryProcess;

public class ReconJudgeOneCellCommand extends Command {

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
            Judgment judgment = Recon.stringToJudgment(request.getParameter("judgment"));
            String identifierSpace = request.getParameter("identifierSpace");
            String schemaSpace = request.getParameter("schemaSpace");

            ReconCandidate match = null;
            String id = request.getParameter("id");
            if (id != null) {
                String scoreString = request.getParameter("score");

                match = new ReconCandidate(
                        id,
                        request.getParameter("name"),
                        request.getParameter("types").split(","),
                        scoreString != null ? Double.parseDouble(scoreString) : 100);
            }

            GridState state = project.getCurrentGridState();
            Cell cell = state.getRow(rowIndex).getCell(cellIndex);
            if (cell == null || !ExpressionUtils.isNonBlankData(cell.value)) {
                throw new Exception("Cell is blank or error");
            }

            ColumnMetadata column = state.getColumnModel().getColumnByIndex(cellIndex);
            if (column == null) {
                throw new Exception("No such column");
            }

            Judgment oldJudgment = cell.recon == null ? Judgment.None : cell.recon.judgment;

            // The historyEntryId will be set on the recon by the CellChange
            Recon newRecon = null;
            if (cell.recon != null) {
                newRecon = cell.recon;
            } else if (identifierSpace != null && schemaSpace != null) {
                newRecon = new Recon(0L, identifierSpace, schemaSpace);
            } else if (column.getReconConfig() != null) {
                newRecon = column.getReconConfig().createNewRecon(0L);
            } else {
                // This should only happen if we are judging a cell in a column that
                // has never been reconciled before.
                // TODO we should rather throw an exception in this case,
                // ReconConfig should be required on the column.
                newRecon = new Recon(0L, null, null);
            }

            String cellDescription = "single cell on row " + (rowIndex + 1) +
                    ", column " + column.getName() +
                    ", containing \"" + cell.value + "\"";

            String description = null;

            newRecon = newRecon
                    .withMatchRank(-1)
                    .withJudgmentAction("single");

            if (judgment == Judgment.None) {
                newRecon = newRecon.withJudgment(Recon.Judgment.None)
                        .withMatch(null);

                description = "Discard recon judgment for " + cellDescription;
            } else if (judgment == Judgment.New) {
                newRecon = newRecon
                        .withJudgment(Recon.Judgment.New)
                        .withMatch(null);

                description = "Mark to create new item for " + cellDescription;
            } else {
                newRecon = newRecon.withJudgment(Recon.Judgment.Matched)
                        .withMatch(match);
                if (newRecon.candidates != null) {
                    for (int m = 0; m < newRecon.candidates.size(); m++) {
                        if (newRecon.candidates.get(m).id.equals(match.id)) {
                            newRecon = newRecon.withMatchRank(m);
                            break;
                        }
                    }
                }

                description = "Match " + match.name +
                        " (" + match.id + ") to " +
                        cellDescription;
            }

            Cell newCell = new Cell(
                    cell.value,
                    newRecon);

            ReconStats stats = column.getReconStats();
            if (stats == null) {
                stats = new LazyReconStats(state, column.getName());
            } else {
                int newChange = 0;
                int matchChange = 0;

                if (oldJudgment == Judgment.New) {
                    newChange--;
                }
                if (oldJudgment == Judgment.Matched) {
                    matchChange--;
                }
                if (newRecon.judgment == Judgment.New) {
                    newChange++;
                }
                if (newRecon.judgment == Judgment.Matched) {
                    matchChange++;
                }

                stats = ReconStats.create(
                        stats.getNonBlanks(),
                        stats.getNewTopics() + newChange,
                        stats.getMatchedTopics() + matchChange);
            }

            Change change = new ReconCellChange(rowIndex, column.getName(), newRecon, stats);

            QuickHistoryEntryProcess process = new QuickHistoryEntryProcess(
                    project.getHistory(),
                    description,
                    null,
                    change);

            HistoryEntry historyEntry = project.getProcessManager().queueProcess(process);
            if (historyEntry != null) {
                /*
                 * If the process is done, write back the cell's data so that the client side can update its UI right
                 * away.
                 */
                respondJSON(response, new ReconClearOneCellCommand.CellResponse(historyEntry, newCell));
            } else {
                respond(response, "{ \"code\" : \"pending\" }");
            }
        } catch (Exception e) {
            respondException(response, e);
        }
    }

}
