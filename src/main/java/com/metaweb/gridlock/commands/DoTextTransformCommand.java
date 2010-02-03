package com.metaweb.gridlock.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import com.metaweb.gridlock.browsing.Engine;
import com.metaweb.gridlock.browsing.FilteredRows;
import com.metaweb.gridlock.browsing.RowVisitor;
import com.metaweb.gridlock.expr.Evaluable;
import com.metaweb.gridlock.expr.Parser;
import com.metaweb.gridlock.history.CellChange;
import com.metaweb.gridlock.history.HistoryEntry;
import com.metaweb.gridlock.history.MassCellChange;
import com.metaweb.gridlock.model.Cell;
import com.metaweb.gridlock.model.Column;
import com.metaweb.gridlock.model.Project;
import com.metaweb.gridlock.model.Row;
import com.metaweb.gridlock.process.QuickHistoryEntryProcess;

public class DoTextTransformCommand extends Command {
	
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
			String expression = request.getParameter("expression");
			
			Evaluable eval = new Parser(expression).getExpression();
			Properties bindings = new Properties();
			bindings.put("project", project);
			
			List<CellChange> cellChanges = new ArrayList<CellChange>(project.rows.size());
			
			FilteredRows filteredRows = engine.getAllFilteredRows();
			filteredRows.accept(project, new RowVisitor() {
				int cellIndex;
				Properties bindings;
				List<CellChange> cellChanges;
				Evaluable eval;
				
				public RowVisitor init(int cellIndex, Properties bindings, List<CellChange> cellChanges, Evaluable eval) {
					this.cellIndex = cellIndex;
					this.bindings = bindings;
					this.cellChanges = cellChanges;
					this.eval = eval;
					return this;
				}
				
				@Override
				public boolean visit(Project project, int rowIndex, Row row) {
					if (cellIndex < row.cells.size()) {
						Cell cell = row.cells.get(cellIndex);
						if (cell.value != null) {
							bindings.put("cell", cell);
							bindings.put("value", cell.value);
							
							Cell newCell = new Cell();
							newCell.value = eval.evaluate(bindings);
							newCell.recon = cell.recon;
							
							CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, newCell);
							cellChanges.add(cellChange);
						}
					}
					return false;
				}
			}.init(cellIndex, bindings, cellChanges, eval));
			
			MassCellChange massCellChange = new MassCellChange(cellChanges, cellIndex);
			HistoryEntry historyEntry = new HistoryEntry(
				project, "Text transform on " + columnName + ": " + expression, massCellChange);
			
			boolean done = project.processManager.queueProcess(
					new QuickHistoryEntryProcess(project, historyEntry));
			
			respond(response, "{ \"code\" : " + (done ? "\"ok\"" : "\"pending\"") + " }");
			
		} catch (Exception e) {
			respondException(response, e);
		}
	}
}
