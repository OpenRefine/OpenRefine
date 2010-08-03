package com.google.gridworks.commands.recon;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.gridworks.commands.EngineDependentCommand;
import com.google.gridworks.model.AbstractOperation;
import com.google.gridworks.model.Project;
import com.google.gridworks.model.recon.ReconConfig;
import com.google.gridworks.operations.recon.ReconOperation;

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
