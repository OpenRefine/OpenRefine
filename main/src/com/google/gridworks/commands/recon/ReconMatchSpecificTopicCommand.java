package com.google.gridworks.commands.recon;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.gridworks.commands.EngineDependentCommand;
import com.google.gridworks.model.AbstractOperation;
import com.google.gridworks.model.Project;
import com.google.gridworks.model.ReconCandidate;
import com.google.gridworks.operations.recon.ReconMatchSpecificTopicOperation;

public class ReconMatchSpecificTopicCommand extends EngineDependentCommand {
    
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        String columnName = request.getParameter("columnName");
        ReconCandidate match = new ReconCandidate(
            request.getParameter("topicID"),
            request.getParameter("topicName"),
            request.getParameter("types").split(","),
            100
        );
        
        return new ReconMatchSpecificTopicOperation(
            engineConfig, 
            columnName, 
            match,
            request.getParameter("identifierSpace"),
            request.getParameter("schemaSpace")
        );
    }
}
