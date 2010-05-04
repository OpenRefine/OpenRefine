package com.metaweb.gridworks.commands.recon;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.metaweb.gridworks.commands.EngineDependentCommand;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.operations.ReconMarkNewTopicsOperation;

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
