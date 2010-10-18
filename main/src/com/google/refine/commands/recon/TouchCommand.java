package com.google.refine.commands.recon;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.recon.TouchOperation;

public class TouchCommand extends EngineDependentCommand {
    
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        return new TouchOperation(engineConfig);
    }
}
