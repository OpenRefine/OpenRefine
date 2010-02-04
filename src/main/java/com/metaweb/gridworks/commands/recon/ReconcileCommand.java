package com.metaweb.gridworks.commands.recon;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.metaweb.gridworks.commands.EngineDependentCommand;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.operations.ReconOperation;

public class ReconcileCommand extends EngineDependentCommand {
	
	@Override
	protected AbstractOperation createOperation(HttpServletRequest request,
			JSONObject engineConfig) throws Exception {
		
		int cellIndex = Integer.parseInt(request.getParameter("cell"));
		String typeID = request.getParameter("type");
		
		return new ReconOperation(engineConfig, cellIndex, typeID);
	}
}
