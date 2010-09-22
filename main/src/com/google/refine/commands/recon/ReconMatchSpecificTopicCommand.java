package com.google.refine.commands.recon;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.model.ReconCandidate;
import com.google.refine.operations.recon.ReconMatchSpecificTopicOperation;

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
