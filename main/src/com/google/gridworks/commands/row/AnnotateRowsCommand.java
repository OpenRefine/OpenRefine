package com.google.gridworks.commands.row;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.gridworks.commands.EngineDependentCommand;
import com.google.gridworks.model.AbstractOperation;
import com.google.gridworks.model.Project;
import com.google.gridworks.operations.row.RowFlagOperation;
import com.google.gridworks.operations.row.RowStarOperation;

public class AnnotateRowsCommand extends EngineDependentCommand {
    
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        String starredString = request.getParameter("starred");
        if (starredString != null) {
            boolean starred = "true".endsWith(starredString);
            
            return new RowStarOperation(engineConfig, starred);
        }
        
        String flaggedString = request.getParameter("flagged");
        if (flaggedString != null) {
            boolean flagged = "true".endsWith(flaggedString);
            
            return new RowFlagOperation(engineConfig, flagged);
        }
        return null;
    }
}
