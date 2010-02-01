package com.metaweb.gridlock.commands;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;

import com.metaweb.gridlock.model.Project;

public class GetProcessesCommand extends Command {
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		Project project = getProject(request);
		
		try {
			Properties options = new Properties();
			
			respondJSON(response, project.processManager.getJSON(options));
		} catch (JSONException e) {
			respondException(response, e);
		}
	}
}
