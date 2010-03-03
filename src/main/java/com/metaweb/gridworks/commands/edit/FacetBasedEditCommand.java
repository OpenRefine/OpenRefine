package com.metaweb.gridworks.commands.edit;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.metaweb.gridworks.commands.EngineDependentCommand;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.operations.FacetBasedEditOperation;
import com.metaweb.gridworks.util.ParsingUtilities;

public class FacetBasedEditCommand extends EngineDependentCommand {
    @Override
    protected AbstractOperation createOperation(HttpServletRequest request,
            JSONObject engineConfig) throws Exception {
        
        String columnName = request.getParameter("columnName");
        String expression = request.getParameter("expression");
        String editsString = request.getParameter("edits");
        
        return new FacetBasedEditOperation(
            engineConfig,
            columnName,
            expression,
            FacetBasedEditOperation.reconstructEdits(ParsingUtilities.evaluateJsonStringToArray(editsString))
        );
    }
}
