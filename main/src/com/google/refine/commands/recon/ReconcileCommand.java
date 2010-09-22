package com.google.refine.commands.recon;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.model.recon.ReconConfig;
import com.google.refine.operations.recon.ReconOperation;

public class ReconcileCommand extends EngineDependentCommand {
    
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        String columnName = request.getParameter("columnName");
        String configString = request.getParameter("config");
        
        JSONTokener t = new JSONTokener(configString);
        JSONObject config = (JSONObject) t.nextValue();
        
        return new ReconOperation(engineConfig, columnName, ReconConfig.reconstruct(config));
    }
}
