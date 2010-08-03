package com.google.gridworks.commands.column;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.gridworks.commands.EngineDependentCommand;
import com.google.gridworks.model.AbstractOperation;
import com.google.gridworks.model.Project;
import com.google.gridworks.operations.cell.TextTransformOperation;
import com.google.gridworks.operations.column.ColumnAdditionOperation;

public class AddColumnCommand extends EngineDependentCommand {
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        String baseColumnName = request.getParameter("baseColumnName");
        String expression = request.getParameter("expression");
        String newColumnName = request.getParameter("newColumnName");
        int columnInsertIndex = Integer.parseInt(request.getParameter("columnInsertIndex"));
        String onError = request.getParameter("onError");
        
        return new ColumnAdditionOperation(
            engineConfig, 
            baseColumnName, 
            expression,
            TextTransformOperation.stringToOnError(onError),
            newColumnName,
            columnInsertIndex
        );
    }

}
