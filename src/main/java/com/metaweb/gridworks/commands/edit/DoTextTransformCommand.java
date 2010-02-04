package com.metaweb.gridworks.commands.edit;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;


import com.metaweb.gridworks.commands.EngineDependentCommand;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.operations.TextTransformOperation;

public class DoTextTransformCommand extends EngineDependentCommand {
	@Override
	protected AbstractOperation createOperation(HttpServletRequest request,
			JSONObject engineConfig) throws Exception {
		
		int cellIndex = Integer.parseInt(request.getParameter("cell"));
		String expression = request.getParameter("expression");
		
		return new TextTransformOperation(engineConfig, cellIndex, expression);
	}
}
