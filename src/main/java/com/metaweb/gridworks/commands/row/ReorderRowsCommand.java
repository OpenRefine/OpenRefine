package com.metaweb.gridworks.commands.row;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.commands.EngineDependentCommand;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.operations.row.RowReorderOperation;
import com.metaweb.gridworks.util.ParsingUtilities;

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
