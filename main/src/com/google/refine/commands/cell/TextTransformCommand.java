package com.google.refine.commands.cell;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.cell.TextTransformOperation;

public class TextTransformCommand extends EngineDependentCommand {
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        String columnName = request.getParameter("columnName");
        String expression = request.getParameter("expression");
        String onError = request.getParameter("onError");
        boolean repeat = "true".equals(request.getParameter("repeat"));
        
        int repeatCount = 10;
        String repeatCountString = request.getParameter("repeatCount");
        try {
            repeatCount = Math.max(Math.min(Integer.parseInt(repeatCountString), 10), 0);
        } catch (Exception e) {
        }
        
        return new TextTransformOperation(
            engineConfig, 
            columnName, 
            expression, 
            TextTransformOperation.stringToOnError(onError),
            repeat,
            repeatCount
        );
    }
}
