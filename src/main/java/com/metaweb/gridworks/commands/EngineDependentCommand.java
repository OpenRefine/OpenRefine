package com.metaweb.gridworks.commands;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.process.Process;

abstract public class EngineDependentCommand extends Command {
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		try {
			Project project = getProject(request);
			
			AbstractOperation op = createOperation(request, getEngineConfig(request));
			Process process = op.createProcess(project, new Properties());
			
			boolean done = project.processManager.queueProcess(process);
			
			respond(response, "{ \"code\" : " + (done ? "\"ok\"" : "\"pending\"") + " }");
			
		} catch (Exception e) {
			respondException(response, e);
		}
	}
	
	abstract protected AbstractOperation createOperation(
			HttpServletRequest request, JSONObject engineConfig) throws Exception;
}
