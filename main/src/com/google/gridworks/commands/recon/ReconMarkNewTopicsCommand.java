package com.google.gridworks.commands.recon;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.gridworks.commands.EngineDependentCommand;
import com.google.gridworks.model.AbstractOperation;
import com.google.gridworks.model.Project;
import com.google.gridworks.operations.recon.ReconMarkNewTopicsOperation;

public class ReconMarkNewTopicsCommand extends EngineDependentCommand {
    
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        return new ReconMarkNewTopicsOperation(
            engineConfig, 
            request.getParameter("columnName"),
            "true".equals(request.getParameter("shareNewTopics"))
        );
    }
}
