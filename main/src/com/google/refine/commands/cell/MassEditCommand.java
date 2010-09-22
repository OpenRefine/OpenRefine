package com.google.refine.commands.cell;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.cell.MassEditOperation;
import com.google.refine.util.ParsingUtilities;

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
