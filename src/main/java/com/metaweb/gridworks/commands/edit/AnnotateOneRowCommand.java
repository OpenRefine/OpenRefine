package com.metaweb.gridworks.commands.edit;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.changes.RowStarChange;
import com.metaweb.gridworks.process.QuickHistoryEntryProcess;

public class AnnotateOneRowCommand extends Command {
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        
		try {
			Project project = getProject(request);
			
			int rowIndex = Integer.parseInt(request.getParameter("row"));
			
			String starredString = request.getParameter("starred");
			if (starredString != null) {
			    boolean starred = "true".endsWith(starredString);
			    String description = starred ? "Star row " + rowIndex : "Unstar row " + rowIndex; 

	            StarOneRowProcess process = new StarOneRowProcess(
                    project, 
                    description,
                    rowIndex, 
                    starred
                );
	            
	            boolean done = project.processManager.queueProcess(process);
	            if (done) {
	                JSONWriter writer = new JSONWriter(response.getWriter());
	                
	                writer.object();
	                writer.key("code"); writer.value("ok");
	                writer.endObject();
	            } else {
	                respond(response, "{ \"code\" : \"pending\" }");
	            }
			} else {
                respond(response, "{ \"code\" : \"error\", \"message\" : \"invalid command parameters\" }");
			}
		} catch (Exception e) {
			respondException(response, e);
		}
	}
	
	protected class StarOneRowProcess extends QuickHistoryEntryProcess {
		final int rowIndex;
		final boolean starred;
		
		StarOneRowProcess(
			Project project, 
			String briefDescription, 
			int rowIndex, 
			boolean starred
		) {
			super(project, briefDescription);
			
			this.rowIndex = rowIndex;
			this.starred = starred;
		}

		protected HistoryEntry createHistoryEntry() throws Exception {
			return new HistoryEntry(
				_project, 
				starred ? "Star row " + rowIndex : "Unstar row " + rowIndex, 
				null, 
				new RowStarChange(rowIndex, starred)
			);
		}
	}
}
