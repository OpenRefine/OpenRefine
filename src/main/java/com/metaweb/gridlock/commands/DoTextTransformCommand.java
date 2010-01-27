package com.metaweb.gridlock.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


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
		
		Project project = getProject(request);
		int cellIndex = Integer.parseInt(request.getParameter("cell"));
		
		String columnName = null;
		for (Column column : project.columnModel.columns) {
			if (column.cellIndex == cellIndex) {
				columnName = column.headerLabel;
				break;
			}
		}
		
		String expression = request.getParameter("expression");
		
		try {
			Evaluable eval = new Parser(expression).getExpression();
			//System.out.println("--- " + eval.toString());
			Properties bindings = new Properties();
			List<CellChange> cellChanges = new ArrayList<CellChange>(project.rows.size());
			
			for (int r = 0; r < project.rows.size(); r++) {
				Row row = project.rows.get(r);
				if (cellIndex < row.cells.size()) {
					Cell cell = row.cells.get(cellIndex);
					if (cell.value == null) {
						continue;
					}
					
					bindings.put("this", cell);
					bindings.put("value", cell.value);
					
					Cell newCell = new Cell();
					newCell.value = eval.evaluate(bindings);
					newCell.recon = cell.recon;
					
					CellChange cellChange = new CellChange(r, cellIndex, cell, newCell);
					cellChanges.add(cellChange);
				}
			}
			
			MassCellChange massCellChange = new MassCellChange(cellChanges);
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
