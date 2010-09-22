package com.google.refine.commands.column;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.column.ExtendDataOperation;
import com.google.refine.util.ParsingUtilities;

public class ExtendDataCommand extends EngineDependentCommand {
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        String baseColumnName = request.getParameter("baseColumnName");
        int columnInsertIndex = Integer.parseInt(request.getParameter("columnInsertIndex"));
        
        String jsonString = request.getParameter("extension");
        JSONObject extension = ParsingUtilities.evaluateJsonStringToObject(jsonString);
        
        return new ExtendDataOperation(
            engineConfig, 
            baseColumnName, 
            extension,
            columnInsertIndex
        );
    }

}
