package com.google.refine.commands.recon;

import javax.servlet.http.HttpServletRequest;

import com.google.refine.browsing.EngineConfig;
import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.recon.ReconUseValuesAsIdentifiersOperation;

public class ReconUseValuesAsIdentifiersCommand extends EngineDependentCommand {
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, EngineConfig engineConfig) throws Exception {
        
        String columnName = request.getParameter("columnName");
        
        return new ReconUseValuesAsIdentifiersOperation(
            engineConfig, 
            columnName, 
            request.getParameter("service"),
            request.getParameter("identifierSpace"),
            request.getParameter("schemaSpace")
        );
    }
}
