package com.metaweb.gridworks.commands.recon;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.metaweb.gridworks.commands.EngineDependentCommand;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.ReconCandidate;
import com.metaweb.gridworks.operations.recon.ReconMatchSpecificTopicOperation;

public class ReconMatchSpecificTopicCommand extends EngineDependentCommand {
    
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        String columnName = request.getParameter("columnName");
        ReconCandidate match = new ReconCandidate(
            request.getParameter("topicID"),
            request.getParameter("topicGUID"),
            request.getParameter("topicName"),
            request.getParameter("types").split(","),
            100
        );
        
        return new ReconMatchSpecificTopicOperation(engineConfig, columnName, match);
    }
}
