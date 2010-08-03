package com.google.gridworks.commands.row;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.gridworks.commands.EngineDependentCommand;
import com.google.gridworks.model.AbstractOperation;
import com.google.gridworks.model.Project;
import com.google.gridworks.operations.row.RowRemovalOperation;

public class RemoveRowsCommand extends EngineDependentCommand {
    
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        return new RowRemovalOperation(engineConfig);
    }
}
