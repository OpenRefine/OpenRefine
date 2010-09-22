package com.google.refine.commands.row;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.row.RowRemovalOperation;

public class RemoveRowsCommand extends EngineDependentCommand {
    
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        return new RowRemovalOperation(engineConfig);
    }
}
