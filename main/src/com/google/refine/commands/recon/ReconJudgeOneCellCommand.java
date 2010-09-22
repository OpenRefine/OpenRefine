package com.google.refine.commands.recon;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

import com.google.refine.commands.Command;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.ReconStats;
import com.google.refine.model.Recon.Judgment;
import com.google.refine.model.changes.CellChange;
import com.google.refine.model.changes.ReconChange;
import com.google.refine.process.QuickHistoryEntryProcess;
import com.google.refine.util.Pool;

public class ReconJudgeOneCellCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            Project project = getProject(request);

            int rowIndex = Integer.parseInt(request.getParameter("row"));
            int cellIndex = Integer.parseInt(request.getParameter("cell"));
            Judgment judgment = Recon.stringToJudgment(request.getParameter("judgment"));

            ReconCandidate match = null;
            String id = request.getParameter("id");
            if (id != null) {
                String scoreString = request.getParameter("score");

                match = new ReconCandidate(
                    id,
                    request.getParameter("name"),
                    request.getParameter("types").split(","),
                    scoreString != null ? Double.parseDouble(scoreString) : 100
                );
            }

            JudgeOneCellProcess process = new JudgeOneCellProcess(
                project,
                "Judge one cell's recon result",
                judgment,
                rowIndex,
                cellIndex,
                match,
                request.getParameter("identifierSpace"),
                request.getParameter("schemaSpace")
            );

            HistoryEntry historyEntry = project.processManager.queueProcess(process);
            if (historyEntry != null) {
                /*
                 * If the process is done, write back the cell's data so that the
                 * client side can update its UI right away.
                 */
                JSONWriter writer = new JSONWriter(response.getWriter());

                Pool pool = new Pool();
                Properties options = new Properties();
                options.put("pool", pool);

                writer.object();
                writer.key("code"); writer.value("ok");
                writer.key("historyEntry"); historyEntry.write(writer, options);
                writer.key("cell"); process.newCell.write(writer, options);
                writer.key("pool"); pool.write(writer, options);
                writer.endObject();
            } else {
                respond(response, "{ \"code\" : \"pending\" }");
            }
        } catch (Exception e) {
            respondException(response, e);
        }
    }

    protected static class JudgeOneCellProcess extends QuickHistoryEntryProcess {

        final int               rowIndex;
        final int               cellIndex;
        final Judgment          judgment;
        final ReconCandidate    match;
        final String            identifierSpace;
        final String            schemaSpace;
        
        Cell newCell;

        JudgeOneCellProcess(
            Project project,
            String briefDescription,
            Judgment judgment,
            int rowIndex,
            int cellIndex,
            ReconCandidate match,
            String identifierSpace,
            String schemaSpace
        ) {
            super(project, briefDescription);

            this.judgment = judgment;
            this.rowIndex = rowIndex;
            this.cellIndex = cellIndex;
            this.match = match;
            this.identifierSpace = identifierSpace;
            this.schemaSpace = schemaSpace;
        }

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

            newCell = new Cell(
                cell.value,
                cell.recon == null ? new Recon(historyEntryID, identifierSpace, schemaSpace) : cell.recon.dup(historyEntryID)
            );

            String cellDescription =
                "single cell on row " + (rowIndex + 1) +
                ", column " + column.getName() +
                ", containing \"" + cell.value + "\"";

            String description = null;

            newCell.recon.matchRank = -1;
            newCell.recon.judgmentAction = "single";
            newCell.recon.judgmentBatchSize = 1;

            if (judgment == Judgment.None) {
                newCell.recon.judgment = Recon.Judgment.None;
                newCell.recon.match = null;

                description = "Discard recon judgment for " + cellDescription;
            } else if (judgment == Judgment.New) {
                newCell.recon.judgment = Recon.Judgment.New;
                newCell.recon.match = null;

                description = "Mark to create new topic for " + cellDescription;
            } else {
                newCell.recon.judgment = Recon.Judgment.Matched;
                newCell.recon.match = this.match;
                if (newCell.recon.candidates != null) {
                    for (int m = 0; m < newCell.recon.candidates.size(); m++) {
                        if (newCell.recon.candidates.get(m).id.equals(this.match.id)) {
                            newCell.recon.matchRank = m;
                            break;
                        }
                    }
                }
                
                description = "Match " + this.match.name +
                    " (" + match.id + ") to " +
                    cellDescription;
            }

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
                if (newCell.recon.judgment == Judgment.New) {
                    newChange++;
                }
                if (newCell.recon.judgment == Judgment.Matched) {
                    matchChange++;
                }

                stats = new ReconStats(
                    stats.nonBlanks,
                    stats.newTopics + newChange,
                    stats.matchedTopics + matchChange);
            }

            Change change = new ReconChange(
                new CellChange(rowIndex, cellIndex, cell, newCell),
                column.getName(),
                column.getReconConfig(),
                stats
            );

            return new HistoryEntry(
                historyEntryID, _project, description, null, change);
        }
    }
}
