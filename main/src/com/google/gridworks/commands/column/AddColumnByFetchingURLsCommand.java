package com.google.gridworks.commands.column;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.gridworks.commands.EngineDependentCommand;
import com.google.gridworks.model.AbstractOperation;
import com.google.gridworks.model.Project;
import com.google.gridworks.operations.cell.TextTransformOperation;
import com.google.gridworks.operations.column.ColumnAdditionByFetchingURLsOperation;

public class AddColumnByFetchingURLsCommand extends EngineDependentCommand {
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        String baseColumnName = request.getParameter("baseColumnName");
        String urlExpression = request.getParameter("urlExpression");
        String newColumnName = request.getParameter("newColumnName");
        int columnInsertIndex = Integer.parseInt(request.getParameter("columnInsertIndex"));
        int delay = Integer.parseInt(request.getParameter("delay"));
        String onError = request.getParameter("onError");
        
        return new ColumnAdditionByFetchingURLsOperation(
            engineConfig, 
            baseColumnName, 
            urlExpression,
            TextTransformOperation.stringToOnError(onError),
            newColumnName,
            columnInsertIndex,
            delay
        );
    }

}
