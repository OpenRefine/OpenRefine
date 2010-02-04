package com.metaweb.gridworks.commands.edit;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.history.HistoryProcess;
import com.metaweb.gridworks.model.Project;

public class UndoRedoCommand extends Command {
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		Project project = getProject(request);
		long lastDoneID = Long.parseLong(request.getParameter("lastDoneID"));
		
		boolean done = project.processManager.queueProcess(
			new HistoryProcess(project, lastDoneID));

		respond(response, "{ \"code\" : " + (done ? "\"ok\"" : "\"pending\"") + " }");
	}
}
