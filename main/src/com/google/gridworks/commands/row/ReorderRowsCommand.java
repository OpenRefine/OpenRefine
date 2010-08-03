package com.google.gridworks.commands.row;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gridworks.browsing.Engine;
import com.google.gridworks.commands.EngineDependentCommand;
import com.google.gridworks.model.AbstractOperation;
import com.google.gridworks.model.Project;
import com.google.gridworks.operations.row.RowReorderOperation;
import com.google.gridworks.util.ParsingUtilities;

public class ReorderRowsCommand extends EngineDependentCommand {
    
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
    	
    	String mode = request.getParameter("mode");
    	JSONObject sorting = null;
    	
        try{
            String json = request.getParameter("sorting");
            
            sorting = (json == null) ? null : ParsingUtilities.evaluateJsonStringToObject(json);
        } catch (JSONException e) {
        	// ignore
        }

        return new RowReorderOperation(Engine.stringToMode(mode), sorting);
    }
}
