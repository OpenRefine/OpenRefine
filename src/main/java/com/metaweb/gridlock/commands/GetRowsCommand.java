package com.metaweb.gridlock.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridlock.browsing.Engine;
import com.metaweb.gridlock.browsing.FilteredRows;
import com.metaweb.gridlock.browsing.RowVisitor;
import com.metaweb.gridlock.model.Project;
import com.metaweb.gridlock.model.Row;

public class GetRowsCommand extends Command {
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		try {
			Project project = getProject(request);
			Engine engine = getEngine(request, project);
			
			int start = Math.min(project.rows.size(), Math.max(0, getIntegerParameter(request, "start", 0)));
			int limit = Math.min(project.rows.size() - start, Math.max(0, getIntegerParameter(request, "limit", 20)));
			Properties options = new Properties();
			
			JSONObject o = new JSONObject();
			
			List<JSONObject> a = new ArrayList<JSONObject>(limit);
			try {
				FilteredRows filteredRows = engine.getAllFilteredRows();
				RowAccumulator acc = new RowAccumulator(start, limit) {
					List<JSONObject> list;
					Properties		 options;
					
					public RowAccumulator init(List<JSONObject> list, Properties options) {
						this.list = list;
						this.options = options;
						return this;
					}
					
					@Override
					public void internalVisit(int rowIndex, Row row) {
						try {
							JSONObject ro = row.getJSON(options);
							ro.put("i", rowIndex);
							list.add(ro);
						} catch (JSONException e) {
						}
					}
				}.init(a, options);
				
				filteredRows.accept(project, acc);
				
				o.put("filtered", acc.total);
			} catch (JSONException e) {
				respondException(response, e);
			}
			o.put("start", start);
			o.put("limit", limit);
			o.put("rows", a);
			o.put("total", project.rows.size());
		
			respondJSON(response, o);
		} catch (Exception e) {
			respondException(response, e);
		}
	}
	
	static protected class RowAccumulator implements RowVisitor {
		final public int start;
		final public int limit;
		
		public int total;
		
		public RowAccumulator(int start, int limit) {
			this.start = start;
			this.limit = limit;
		}
		
		@Override
		public void visit(int rowIndex, Row row) {
			if (total >= start && total < start + limit) {
				internalVisit(rowIndex, row);
			}
			total++;
		}
		
		protected void internalVisit(int rowIndex, Row row) {
		}
	}
}
