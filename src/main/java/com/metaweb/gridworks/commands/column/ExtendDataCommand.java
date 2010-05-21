package com.metaweb.gridworks.commands.column;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.metaweb.gridworks.commands.EngineDependentCommand;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.operations.column.ExtendDataOperation;
import com.metaweb.gridworks.util.ParsingUtilities;

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
