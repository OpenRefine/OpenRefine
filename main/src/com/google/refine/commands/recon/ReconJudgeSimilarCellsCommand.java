package com.google.refine.commands.recon;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.Recon.Judgment;
import com.google.refine.operations.recon.ReconJudgeSimilarCellsOperation;

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
