package com.metaweb.gridworks.commands.recon;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.ReconCandidate;
import com.metaweb.gridworks.model.changes.CellChange;
import com.metaweb.gridworks.process.Process;
import com.metaweb.gridworks.process.QuickHistoryEntryProcess;

public class JudgeOneCellCommand extends Command {
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		try {
			Project project = getProject(request);
			
			int rowIndex = Integer.parseInt(request.getParameter("row"));
			int cellIndex = Integer.parseInt(request.getParameter("cell"));
			Cell cell = project.rows.get(rowIndex).getCell(cellIndex);
			if (cell == null || cell.value == null) {
				respond(response, "{ \"code\" : \"error\", \"message\" : \"Cell is blank\" }");
				return;
			}
			
			Column column = project.columnModel.getColumnByCellIndex(cellIndex);
			if (column == null) {
				respond(response, "{ \"code\" : \"error\", \"message\" : \"No such column\" }");
				return;
			}
			
			Cell newCell = new Cell(
				cell.value, 
				cell.recon == null ? new Recon() : cell.recon.dup()
			);
			
			String cellDescription = 
				"single cell on row " + (rowIndex + 1) + 
				", column " + column.getHeaderLabel() + 
				", containing \"" + cell.value + "\"";
			
			String description = null;
			
			String judgment = request.getParameter("judgment");
			if ("match".equals(judgment)) {
				ReconCandidate match = null;
				
				if (cell.recon != null) {
					String candidateID = request.getParameter("candidate");
					
					for (ReconCandidate c : cell.recon.candidates) {
						if (candidateID.equals(c.topicID)) {
							match = c;
							break;
						}
					}
				}
				if (match == null) {
					respond(response, "{ \"code\" : \"error\", \"message\" : \"No such recon candidate\" }");
					return;
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
			} else {
				respond(response, "{ \"code\" : \"error\", \"message\" : \"bad judgment\" }");
				return;
			}
			
			Change change = new CellChange(rowIndex, cellIndex, cell, newCell);
			HistoryEntry historyEntry = new HistoryEntry(
				project, description, null, change);

			Process process = new QuickHistoryEntryProcess(project, historyEntry);
			
			boolean done = project.processManager.queueProcess(process);
			if (done) {
				JSONWriter writer = new JSONWriter(response.getWriter());
				writer.object();
				writer.key("code"); writer.value("ok");
				writer.key("cell"); newCell.write(writer, new Properties());
				writer.endObject();
			} else {
				respond(response, "{ \"code\" : \"pending\" }");
			}
		} catch (Exception e) {
			respondException(response, e);
		}
	}
}
