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
import com.metaweb.gridworks.model.changes.CellChange;
import com.metaweb.gridworks.process.QuickHistoryEntryProcess;

public class ReconJudgeOneCellCommand extends Command {
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		try {
			Project project = getProject(request);
			
			int rowIndex = Integer.parseInt(request.getParameter("row"));
			int cellIndex = Integer.parseInt(request.getParameter("cell"));
			String judgment = request.getParameter("judgment"); 

			JudgeOneCellProcess process = null;
			
			if (judgment != null) {
				process = new JudgeOneCellProcess(
					project, 
					"Judge one cell's recon result",
					judgment,
					rowIndex, 
					cellIndex, 
					request.getParameter("candidate")
				);
			} else {
				ReconCandidate match = new ReconCandidate(
					request.getParameter("topicID"),
					request.getParameter("topicGUID"),
					request.getParameter("topicName"),
					request.getParameter("types").split(","),
					100
				);
				
				process = new JudgeOneCellProcess(
					project, 
					"Judge one cell's recon result",
					rowIndex, 
					cellIndex, 
					match
				);
			}
			
			boolean done = project.processManager.queueProcess(process);
			if (done) {
				JSONWriter writer = new JSONWriter(response.getWriter());
				writer.object();
				writer.key("code"); writer.value("ok");
				writer.key("cell"); process.newCell.write(writer, new Properties());
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
		final String judgment;
		final String candidateID;
		final ReconCandidate match;
		Cell newCell;
		
		
		JudgeOneCellProcess(Project project, String briefDescription, String judgment, int rowIndex, int cellIndex, String candidateID) {
			super(project, briefDescription);
			this.rowIndex = rowIndex;
			this.cellIndex = cellIndex;
			this.judgment = judgment;
			this.candidateID = candidateID;
			this.match = null;
		}
		
		JudgeOneCellProcess(Project project, String briefDescription, int rowIndex, int cellIndex, ReconCandidate match) {
			super(project, briefDescription);
			this.rowIndex = rowIndex;
			this.cellIndex = cellIndex;
			this.judgment = null;
			this.candidateID = null;
			this.match = match;
		}

		protected HistoryEntry createHistoryEntry() throws Exception {
			Cell cell = _project.rows.get(rowIndex).getCell(cellIndex);
            if (cell == null || ExpressionUtils.isBlank(cell.value)) {
				throw new Exception("Cell is blank");
			}
			
			Column column = _project.columnModel.getColumnByCellIndex(cellIndex);
			if (column == null) {
				throw new Exception("No such column");
			}
			
			newCell = new Cell(
				cell.value, 
				cell.recon == null ? new Recon() : cell.recon.dup()
			);
			
			String cellDescription = 
				"single cell on row " + (rowIndex + 1) + 
				", column " + column.getHeaderLabel() + 
				", containing \"" + cell.value + "\"";
			
			String description = null;
			
			if (match != null) {
				newCell.recon.judgment = Recon.Judgment.Matched;
				newCell.recon.match = this.match;
				
				description = "Match " + this.match.topicName +
					" (" + match.topicID + ") to " + 
					cellDescription;
			} else {
				if ("match".equals(judgment)) {
					ReconCandidate match = null;
					
					if (cell.recon != null) {
						for (ReconCandidate c : cell.recon.candidates) {
							if (candidateID.equals(c.topicID)) {
								match = c;
								break;
							}
						}
					}
					if (match == null) {
						throw new Exception("No such recon candidate");
					}
					
					newCell.recon.judgment = Recon.Judgment.Matched;
					newCell.recon.match = match;
					
					description = "Match " + match.topicName +
						" (" + match.topicID + ") to " + 
						cellDescription;
					
				} else if ("new".equals(judgment)) {
					newCell.recon.judgment = Recon.Judgment.New;
					description = "Mark to create new topic for " + cellDescription;
				} else if ("discard".equals(judgment)) {
					newCell.recon.judgment = Recon.Judgment.None;
					newCell.recon.match = null;
					description = "Discard recon judgment for " + cellDescription;
				}
			}
			
			Change change = new CellChange(rowIndex, cellIndex, cell, newCell);
			
			return new HistoryEntry(
				_project, description, null, change);
		}
	}
}
