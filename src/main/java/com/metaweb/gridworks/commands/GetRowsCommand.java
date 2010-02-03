package com.metaweb.gridworks.commands;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

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
			
			response.setCharacterEncoding("UTF-8");
	    	response.setHeader("Content-Type", "application/json");
	    	
			JSONWriter writer = new JSONWriter(response.getWriter());
			writer.object();
			
			{
				RowAccumulator acc = new RowAccumulator(start, limit) {
					JSONWriter	writer;
					Properties	options;
					
					public RowAccumulator init(JSONWriter writer, Properties options) {
						this.writer = writer;
						this.options = options;
						return this;
					}
					
					@Override
					public boolean internalVisit(int rowIndex, Row row) {
						try {
							options.put("rowIndex", rowIndex);
							row.write(writer, options);
						} catch (JSONException e) {
						}
						return false;
					}
				}.init(writer, options);
				
				FilteredRows filteredRows = engine.getAllFilteredRows();
				
				writer.key("rows"); writer.array();
				filteredRows.accept(project, acc);
				writer.endArray();
				
				writer.key("filtered"); writer.value(acc.total);
			}
			
			writer.key("start"); writer.value(start);
			writer.key("limit"); writer.value(limit);
			writer.key("total"); writer.value(project.rows.size());
			
			writer.endObject();
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
		public boolean visit(Project project, int rowIndex, Row row) {
			boolean r = false;
			
			if (total >= start && total < start + limit) {
				r = internalVisit(rowIndex, row);
			}
			total++;
			return r;
		}
		
		protected boolean internalVisit(int rowIndex, Row row) {
			return false;
		}
	}
}
