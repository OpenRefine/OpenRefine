package com.metaweb.gridlock.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import com.metaweb.gridlock.expr.Evaluable;
import com.metaweb.gridlock.expr.FieldAccessorExpr;
import com.metaweb.gridlock.expr.Function;
import com.metaweb.gridlock.expr.FunctionCallExpr;
import com.metaweb.gridlock.expr.LiteralExpr;
import com.metaweb.gridlock.expr.VariableExpr;
import com.metaweb.gridlock.expr.functions.Replace;
import com.metaweb.gridlock.expr.functions.ToLowercase;
import com.metaweb.gridlock.expr.functions.ToTitlecase;
import com.metaweb.gridlock.expr.functions.ToUppercase;
import com.metaweb.gridlock.history.CellChange;
import com.metaweb.gridlock.history.HistoryEntry;
import com.metaweb.gridlock.history.MassCellChange;
import com.metaweb.gridlock.model.Cell;
import com.metaweb.gridlock.model.Project;
import com.metaweb.gridlock.model.Row;
import com.metaweb.gridlock.process.QuickHistoryEntryProcess;

public class DoTextTransformCommand extends Command {
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		Project project = getProject(request);
		int cellIndex = Integer.parseInt(request.getParameter("cell"));
		
		String expression = request.getParameter("expression");
		
		// HACK: quick hack before we implement a parser
		
		Evaluable eval = null;
		if (expression.startsWith("replace(this.value,")) {
			// HACK: huge hack
			
			String s = "[" + expression.substring(
					"replace(this.value,".length(), expression.length() - 1) + "]";
			
			try {
		    	JSONTokener t = new JSONTokener(s);
		    	JSONArray a = (JSONArray) t.nextValue();
		    	
				eval = new FunctionCallExpr(new Evaluable[] {
						new FieldAccessorExpr(new VariableExpr("this"), "value"),
						new LiteralExpr(a.get(0)),
						new LiteralExpr(a.get(1))
					}, new Replace());
				
			} catch (JSONException e) {
			}
		} else {
			Function f = null;
			if (expression.equals("toUppercase(this.value)")) {
				f = new ToUppercase();
			} else if (expression.equals("toLowercase(this.value)")) {
				f = new ToLowercase();
			} else if (expression.equals("toTitlecase(this.value)")) {
				f = new ToTitlecase();
			}
		
			eval = new FunctionCallExpr(new Evaluable[] {
				new FieldAccessorExpr(new VariableExpr("this"), "value")
			}, f);
		}
		
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
				
				Cell newCell = new Cell();
				newCell.value = eval.evaluate(bindings);
				newCell.recon = cell.recon;
				
				CellChange cellChange = new CellChange(r, cellIndex, cell, newCell);
				cellChanges.add(cellChange);
			}
		}
		
		MassCellChange massCellChange = new MassCellChange(cellChanges);
		HistoryEntry historyEntry = new HistoryEntry(project, "Text transform: " + expression, massCellChange);
		
		boolean done = project.processManager.queueProcess(
				new QuickHistoryEntryProcess(project, historyEntry));
		
		respond(response, "{ \"code\" : " + (done ? "\"ok\"" : "\"pending\"") + " }");
	}
}
