package com.google.refine.commands.column;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.column.ColumnReorderOperation;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

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
