package com.metaweb.gridworks.commands.edit;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import com.metaweb.gridworks.commands.EngineDependentCommand;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.operations.ColumnSplitOperation;
import com.metaweb.gridworks.util.ParsingUtilities;

public class SplitColumnCommand extends EngineDependentCommand {
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        String columnName = request.getParameter("columnName");
        boolean guessCellType = Boolean.parseBoolean(request.getParameter("guessCellType"));
        boolean removeOriginalColumn = Boolean.parseBoolean(request.getParameter("removeOriginalColumn"));
        String mode = request.getParameter("mode");
        if ("separator".equals(mode)) {
            String maxColumns = request.getParameter("maxColumns");
            
            return new ColumnSplitOperation(
                engineConfig, 
                columnName, 
                guessCellType,
                removeOriginalColumn,
                request.getParameter("separator"),
                Boolean.parseBoolean(request.getParameter("regex")),
                maxColumns != null && maxColumns.length() > 0 ? Integer.parseInt(maxColumns) : 0
            );
        } else {
            String s = request.getParameter("fieldLengths");
            
            JSONArray a = ParsingUtilities.evaluateJsonStringToArray(s);
            int[] fieldLengths = new int[a.length()];
            
            for (int i = 0; i < fieldLengths.length; i++) {
                fieldLengths[i] = a.getInt(i);
            }
            
            return new ColumnSplitOperation(
                engineConfig, 
                columnName, 
                guessCellType,
                removeOriginalColumn,
                fieldLengths
            );
        }
    }
}
