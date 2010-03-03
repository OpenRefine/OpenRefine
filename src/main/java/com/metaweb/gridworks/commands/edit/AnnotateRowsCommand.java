package com.metaweb.gridworks.commands.edit;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.metaweb.gridworks.commands.EngineDependentCommand;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.operations.RowStarOperation;

public class AnnotateRowsCommand extends EngineDependentCommand {
    
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        String starredString = request.getParameter("starred");
        if (starredString != null) {
            boolean starred = "true".endsWith(starredString);
            
            return new RowStarOperation(engineConfig, starred);
        }
        return null;
    }
}
