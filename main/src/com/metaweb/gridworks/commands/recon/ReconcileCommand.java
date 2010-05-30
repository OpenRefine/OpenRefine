package com.metaweb.gridworks.commands.recon;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.json.JSONTokener;

import com.metaweb.gridworks.commands.EngineDependentCommand;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.recon.ReconConfig;
import com.metaweb.gridworks.operations.recon.ReconOperation;

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
