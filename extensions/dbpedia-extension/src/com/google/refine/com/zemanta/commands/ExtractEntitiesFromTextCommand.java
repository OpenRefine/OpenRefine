package com.google.refine.com.zemanta.commands;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.com.zemanta.operations.ExtractEntitiesFromTextOperation;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

public class ExtractEntitiesFromTextCommand extends EngineDependentCommand {
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        String baseColumnName = request.getParameter("baseColumnName");
        int columnInsertIndex = Integer.parseInt(request.getParameter("columnInsertIndex"));
        
        String jsonString = request.getParameter("extension");
        JSONObject extension = ParsingUtilities.evaluateJsonStringToObject(jsonString);
        
        return new ExtractEntitiesFromTextOperation(
            engineConfig, 
            baseColumnName, 
            extension,
            columnInsertIndex
        );
    }

}
