package com.google.gridworks.commands.column;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.gridworks.commands.EngineDependentCommand;
import com.google.gridworks.model.AbstractOperation;
import com.google.gridworks.model.Project;
import com.google.gridworks.operations.column.ExtendDataOperation;
import com.google.gridworks.util.ParsingUtilities;

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
