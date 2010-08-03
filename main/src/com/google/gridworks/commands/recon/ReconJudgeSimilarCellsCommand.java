package com.google.gridworks.commands.recon;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.gridworks.commands.EngineDependentCommand;
import com.google.gridworks.model.AbstractOperation;
import com.google.gridworks.model.Project;
import com.google.gridworks.model.Recon;
import com.google.gridworks.model.ReconCandidate;
import com.google.gridworks.model.Recon.Judgment;
import com.google.gridworks.operations.recon.ReconJudgeSimilarCellsOperation;

public class ReconJudgeSimilarCellsCommand extends EngineDependentCommand {

    @Override
    protected AbstractOperation createOperation(
            Project project, HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        String columnName = request.getParameter("columnName");
        String similarValue = request.getParameter("similarValue");
        Judgment judgment = Recon.stringToJudgment(request.getParameter("judgment"));
        
        ReconCandidate match = null;
        String id = request.getParameter("id");
        if (id != null) {
            String scoreString = request.getParameter("score");
            
            match = new ReconCandidate(
                id,
                request.getParameter("name"),
                request.getParameter("types").split(","),
                scoreString != null ? Double.parseDouble(scoreString) : 100
            );
        }
        
        String shareNewTopics = request.getParameter("shareNewTopics");
        
        return new ReconJudgeSimilarCellsOperation(
            engineConfig, 
            columnName,
            similarValue,
            judgment,
            match,
            "true".equals(shareNewTopics)
        );
    }
}
