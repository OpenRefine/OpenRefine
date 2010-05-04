package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.InterProjectModel.ProjectJoin;
import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.expr.WrappedCell;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;
import com.metaweb.gridworks.model.Project;

public class Cross implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 3) {
            // from project is implied
            
            Object wrappedCell = args[0]; // from cell
            Object toProjectName = args[1];
            Object toColumnName = args[2];
            
            if (wrappedCell != null && wrappedCell instanceof WrappedCell &&
                toProjectName != null && toProjectName instanceof String &&
                toColumnName != null && toColumnName instanceof String) {
                
                ProjectJoin join = ProjectManager.singleton.getInterProjectModel().getJoin(
                    ProjectManager.singleton.getProjectMetadata(
                            ((Project) bindings.get("project")).id).getName(),
                    ((WrappedCell) wrappedCell).columnName,
                    (String) toProjectName,
                    (String) toColumnName
                );
                
                return join.getRows(((WrappedCell) wrappedCell).cell.value);
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a cell, a project name to join with, and a column name in that project");
    }

    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("TODO");
        writer.key("params"); writer.value("cell c, string projectName, string columnName");
        writer.key("returns"); writer.value("array");
        writer.endObject();
    }
}
