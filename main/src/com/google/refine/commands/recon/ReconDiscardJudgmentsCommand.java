package com.google.refine.commands.recon;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.recon.ReconDiscardJudgmentsOperation;

public class ReconDiscardJudgmentsCommand extends EngineDependentCommand {
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        String columnName = request.getParameter("columnName");
        
        return new ReconDiscardJudgmentsOperation(engineConfig, columnName);
    }
}
