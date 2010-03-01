package com.metaweb.gridworks.expr.functions.strings;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.ProjectMetadata;
import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;
import com.metaweb.gridworks.model.Project;

public class Reinterpret implements Function {

	public Object call(Properties bindings, Object[] args) {
        if (args.length == 2) {
            Object o1 = args[0];
            Object o2 = args[1];
            if (o1 != null && o2 != null && o2 instanceof String) {
                String str = (o1 instanceof String) ? (String) o1 : o1.toString();
                Project project = (Project) bindings.get("project");
                ProjectMetadata metadata = ProjectManager.singleton.getProjectMetadata(project.id);
                String decoder = (String) metadata.getEncoding();
                String encoder = (String) o2;
                String reinterpreted = null;

                try {
                    reinterpreted = new String(str.getBytes(decoder), encoder);
                } catch (UnsupportedEncodingException e) {
                    return new EvalError(ControlFunctionRegistry.getFunctionName(this) + ": encoding '" + encoder + "' is not available or recognized.");
                }
                                
                return reinterpreted;
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects 2 arguments");
	}
	
	public void write(JSONWriter writer, Properties options)
		throws JSONException {
	
		writer.object();
		writer.key("description"); writer.value("Returns s reinterpreted thru the given encoder.");
		writer.key("params"); writer.value("string s, string encoder");
		writer.key("returns"); writer.value("string");
		writer.endObject();
	}
}
