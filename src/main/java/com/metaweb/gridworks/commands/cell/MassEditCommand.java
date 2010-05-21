package com.metaweb.gridworks.commands.cell;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.metaweb.gridworks.commands.EngineDependentCommand;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.operations.cell.MassEditOperation;
import com.metaweb.gridworks.util.ParsingUtilities;

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
