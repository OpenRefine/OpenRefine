package com.metaweb.gridworks.commands.info;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class ExportRowsCommand extends Command {

    public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		try {
			Project project = getProject(request);
			Engine engine = getEngine(request, project);
			
			response.setCharacterEncoding("UTF-8");
	    	response.setHeader("Content-Type", "text/plain");
	    	
			PrintWriter writer = response.getWriter();
			
			boolean first = true;
			for (Column column : project.columnModel.columns) {
				if (first) {
					first = false;
				} else {
					writer.print("\t");
				}
				writer.print(column.getHeaderLabel());
			}
			writer.print("\n");
			
			{
				RowVisitor visitor = new RowVisitor() {
					PrintWriter	writer;
					
					public RowVisitor init(PrintWriter writer) {
						this.writer = writer;
						return this;
					}
					
					public boolean visit(Project project, int rowIndex, Row row, boolean contextual) {
						boolean first = true;
						for (Column column : project.columnModel.columns) {
							if (first) {
								first = false;
							} else {
								writer.print("\t");
							}
							
							int cellIndex = column.getCellIndex();
							if (cellIndex < row.cells.size()) {
								Cell cell = row.cells.get(cellIndex);
								if (cell != null && cell.value != null) {
									writer.print(cell.value);
								}
							}
						}
						writer.print("\n");
						
						return false;
					}
				}.init(writer);
				
				FilteredRows filteredRows = engine.getAllFilteredRows(true);
				filteredRows.accept(project, visitor);
			}
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
		
		public boolean visit(Project project, int rowIndex, Row row, boolean contextual) {
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
