package com.metaweb.gridworks.commands.edit;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;


import com.metaweb.gridworks.commands.EngineDependentCommand;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.operations.TextTransformOperation;

public class DoTextTransformCommand extends EngineDependentCommand {
	@Override
	protected AbstractOperation createOperation(HttpServletRequest request,
			JSONObject engineConfig) throws Exception {
		
		String columnName = request.getParameter("columnName");
		String expression = request.getParameter("expression");
		
		return new TextTransformOperation(engineConfig, columnName, expression);
	}
}
