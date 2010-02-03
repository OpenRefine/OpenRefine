package com.metaweb.gridworks.commands;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONWriter;


import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.Parser;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class PreviewExpressionCommand extends Command {
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		try {
			Project project = getProject(request);
			
			int cellIndex = Integer.parseInt(request.getParameter("cellIndex"));
			
			String expression = request.getParameter("expression");
			Evaluable eval = new Parser(expression).getExpression();
			
			String rowIndicesString = request.getParameter("rowIndices");
			if (rowIndicesString == null) {
				respond(response, "{ \"code\" : \"error\", \"message\" : \"No row indices specified\" }");
				return;
			}
			
			JSONArray rowIndices = jsonStringToArray(rowIndicesString);
			int length = rowIndices.length();
			
			JSONWriter writer = new JSONWriter(response.getWriter());
			
			writer.object();
			writer.key("code"); writer.value("ok");
			writer.key("results"); writer.array();
			
			Properties bindings = new Properties();
			bindings.put("project", project);
			for (int i = 0; i < length; i++) {
				Object result = null;
				
				int rowIndex = rowIndices.getInt(i);
				if (rowIndex >= 0 && rowIndex < project.rows.size()) {
					Row row = project.rows.get(rowIndex);
					if (cellIndex < row.cells.size()) {
						Cell cell = row.cells.get(cellIndex);
						if (cell.value != null) {
							bindings.put("cell", cell);
							bindings.put("value", cell.value);
							
							try {
								result = eval.evaluate(bindings);
							} catch (Exception e) {
								// ignore
							}
						}
					}
				}
				
				writer.value(result);
			}
			writer.endArray();
			writer.endObject();
		} catch (Exception e) {
			respondException(response, e);
		}
	}
}
