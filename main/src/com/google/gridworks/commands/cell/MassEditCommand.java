package com.google.gridworks.commands.cell;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.gridworks.commands.EngineDependentCommand;
import com.google.gridworks.model.AbstractOperation;
import com.google.gridworks.model.Project;
import com.google.gridworks.operations.cell.MassEditOperation;
import com.google.gridworks.util.ParsingUtilities;

public class MassEditCommand extends EngineDependentCommand {
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        String columnName = request.getParameter("columnName");
        String expression = request.getParameter("expression");
        String editsString = request.getParameter("edits");
        
        return new MassEditOperation(
            engineConfig,
            columnName,
            expression,
            MassEditOperation.reconstructEdits(ParsingUtilities.evaluateJsonStringToArray(editsString))
        );
    }
}
