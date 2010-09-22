package com.google.refine.commands.row;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.refine.browsing.Engine;
import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.row.RowReorderOperation;
import com.google.refine.util.ParsingUtilities;

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
