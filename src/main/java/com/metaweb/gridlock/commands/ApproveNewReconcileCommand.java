package com.metaweb.gridlock.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metaweb.gridlock.browsing.Engine;
import com.metaweb.gridlock.browsing.FilteredRows;
import com.metaweb.gridlock.browsing.RowVisitor;
import com.metaweb.gridlock.history.CellChange;
import com.metaweb.gridlock.history.HistoryEntry;
import com.metaweb.gridlock.history.MassCellChange;
import com.metaweb.gridlock.model.Cell;
import com.metaweb.gridlock.model.Column;
import com.metaweb.gridlock.model.Project;
import com.metaweb.gridlock.model.Recon;
import com.metaweb.gridlock.model.Row;
import com.metaweb.gridlock.model.Recon.Judgment;
import com.metaweb.gridlock.process.QuickHistoryEntryProcess;

public class ApproveNewReconcileCommand extends Command {
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		try {
			Project project = getProject(request);
			Engine engine = getEngine(request, project);
			
			int cellIndex = Integer.parseInt(request.getParameter("cell"));
			Column column = project.columnModel.getColumnByCellIndex(cellIndex);
			if (column == null) {
				respond(response, "{ \"code\" : \"error\", \"message\" : \"No such column\" }");
				return;
			}
			
			String columnName = column.headerLabel;
			List<CellChange> cellChanges = new ArrayList<CellChange>(project.rows.size());
			
			FilteredRows filteredRows = engine.getAllFilteredRows();
			filteredRows.accept(project, new RowVisitor() {
				int cellIndex;
				List<CellChange> cellChanges;
				
				public RowVisitor init(int cellIndex, List<CellChange> cellChanges) {
					this.cellIndex = cellIndex;
					this.cellChanges = cellChanges;
					return this;
				}
				
				@Override
				public boolean visit(Project project, int rowIndex, Row row) {
					if (cellIndex < row.cells.size()) {
						Cell cell = row.cells.get(cellIndex);
						
						Cell newCell = new Cell();
						newCell.value = cell.value;
						newCell.recon = cell.recon != null ? cell.recon.dup() : new Recon();
						newCell.recon.match = null;
						newCell.recon.judgment = Judgment.New;
						
						CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, newCell);
						cellChanges.add(cellChange);
					}
					return false;
				}
			}.init(cellIndex, cellChanges));
			
			MassCellChange massCellChange = new MassCellChange(cellChanges);
			HistoryEntry historyEntry = new HistoryEntry(
				project, "Approve new topics for " + columnName, massCellChange);
			
			boolean done = project.processManager.queueProcess(
					new QuickHistoryEntryProcess(project, historyEntry));
			
			respond(response, "{ \"code\" : " + (done ? "\"ok\"" : "\"pending\"") + " }");
		} catch (Exception e) {
			respondException(response, e);
		}
	}
}
