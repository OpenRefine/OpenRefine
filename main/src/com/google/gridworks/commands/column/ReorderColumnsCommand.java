package com.google.gridworks.commands.column;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.gridworks.commands.EngineDependentCommand;
import com.google.gridworks.model.AbstractOperation;
import com.google.gridworks.model.Project;
import com.google.gridworks.operations.column.ColumnReorderOperation;
import com.google.gridworks.util.JSONUtilities;
import com.google.gridworks.util.ParsingUtilities;

public class ReorderColumnsCommand extends EngineDependentCommand {
    
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        String columnNames = request.getParameter("columnNames");
        return new ColumnReorderOperation(
                JSONUtilities.toStringList(
                        ParsingUtilities.evaluateJsonStringToArray(columnNames)));
    }
}
