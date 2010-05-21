package com.metaweb.gridworks.commands.column;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.metaweb.gridworks.commands.EngineDependentCommand;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.operations.cell.TextTransformOperation;
import com.metaweb.gridworks.operations.column.ColumnAdditionOperation;

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
