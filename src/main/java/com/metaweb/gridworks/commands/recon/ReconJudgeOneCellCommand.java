package com.metaweb.gridworks.commands.recon;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.ReconCandidate;
import com.metaweb.gridworks.model.ReconStats;
import com.metaweb.gridworks.model.Recon.Judgment;
import com.metaweb.gridworks.model.changes.CellChange;
import com.metaweb.gridworks.model.changes.ReconChange;
import com.metaweb.gridworks.process.QuickHistoryEntryProcess;

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
            String topicID = request.getParameter("topicID");
            if (topicID != null) {
                String scoreString = request.getParameter("score");
                
                match = new ReconCandidate(
                    topicID,
                    request.getParameter("topicGUID"),
                    request.getParameter("topicName"),
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
                match
            );
            
            HistoryEntry historyEntry = project.processManager.queueProcess(process);
            if (historyEntry != null) {
                /*
                 * If the process is done, write back the cell's data so that the
                 * client side can update its UI right away.
                 */
                JSONWriter writer = new JSONWriter(response.getWriter());
                Properties options = new Properties();
                
                writer.object();
                writer.key("code"); writer.value("ok");
                writer.key("historyEntry"); historyEntry.write(writer, options);
                writer.key("cell"); process.newCell.write(writer, options);
                writer.endObject();
            } else {
                respond(response, "{ \"code\" : \"pending\" }");
            }
        } catch (Exception e) {
            respondException(response, e);
        }
    }
    
    protected class JudgeOneCellProcess extends QuickHistoryEntryProcess {
        final int rowIndex;
        final int cellIndex;
        final Judgment judgment;
        final ReconCandidate match;
        Cell newCell;
        
        
        JudgeOneCellProcess(
            Project project, 
            String briefDescription, 
            Judgment judgment, 
            int rowIndex, 
            int cellIndex, 
            ReconCandidate match
        ) {
            super(project, briefDescription);
            
            this.judgment = judgment;
            this.rowIndex = rowIndex;
            this.cellIndex = cellIndex;
            this.match = match;
        }

        protected HistoryEntry createHistoryEntry() throws Exception {
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
                cell.recon == null ? new Recon() : cell.recon.dup()
            );
            
            String cellDescription = 
                "single cell on row " + (rowIndex + 1) + 
                ", column " + column.getName() + 
                ", containing \"" + cell.value + "\"";
            
            String description = null;
            if (judgment == Judgment.None) {
                newCell.recon.judgment = Recon.Judgment.None;
                newCell.recon.match = null;
                description = "Discard recon judgment for " + cellDescription;
            } else if (judgment == Judgment.New) {
                newCell.recon.judgment = Recon.Judgment.New;
                description = "Mark to create new topic for " + cellDescription;
            } else {
                newCell.recon.judgment = Recon.Judgment.Matched;
                newCell.recon.match = this.match;
                
                description = "Match " + this.match.topicName +
                    " (" + match.topicID + ") to " + 
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
                _project, description, null, change);
        }
    }
}
