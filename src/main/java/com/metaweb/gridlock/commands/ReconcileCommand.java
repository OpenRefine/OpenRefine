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
import com.metaweb.gridlock.model.Cell;
import com.metaweb.gridlock.model.Column;
import com.metaweb.gridlock.model.Project;
import com.metaweb.gridlock.model.Row;
import com.metaweb.gridlock.process.ReconProcess;
import com.metaweb.gridlock.process.ReconProcess.ReconEntry;

public class ReconcileCommand extends Command {
	
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
			String typeID = request.getParameter("type");
			
			List<ReconEntry> entries = new ArrayList<ReconEntry>(project.rows.size());
			
			FilteredRows filteredRows = engine.getAllFilteredRows();
			filteredRows.accept(project, new RowVisitor() {
				int cellIndex;
				List<ReconEntry> entries;
				
				public RowVisitor init(int cellIndex, List<ReconEntry> entries) {
					this.cellIndex = cellIndex;
					this.entries = entries;
					return this;
				}
				
				@Override
				public boolean visit(Project project, int rowIndex, Row row) {
					if (cellIndex < row.cells.size()) {
						Cell cell = row.cells.get(cellIndex);
						if (cell.value != null) {
							entries.add(new ReconEntry(rowIndex, cell));
						}
					}
					return false;
				}
			}.init(cellIndex, entries));
			
			ReconProcess process = new ReconProcess(
				project,
				"Reconcile " + columnName + " to type " + typeID,
				cellIndex,
				entries,
				typeID
			);
			
			boolean done = project.processManager.queueProcess(process);
			
			respond(response, "{ \"code\" : " + (done ? "\"ok\"" : "\"pending\"") + " }");
			
		} catch (Exception e) {
			respondException(response, e);
		}
	}
}
