package com.metaweb.gridworks.commands.edit;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.operations.OperationRegistry;
import com.metaweb.gridworks.process.Process;
import com.metaweb.gridworks.util.ParsingUtilities;

public class ApplyOperationsCommand extends Command {
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        Project project = getProject(request);
        String jsonString = request.getParameter("operations");
        try {
            JSONArray a = ParsingUtilities.evaluateJsonStringToArray(jsonString);
            int count = a.length();
            for (int i = 0; i < count; i++) {
                JSONObject obj = a.getJSONObject(i);
                
                reconstructOperation(project, obj);
            }

            if (project.processManager.hasPending()) {
                respond(response, "{ \"code\" : \"pending\" }");
            } else {
                respond(response, "{ \"code\" : \"ok\" }");
            }
        } catch (JSONException e) {
            respondException(response, e);
        }
    }
    
    protected void reconstructOperation(Project project, JSONObject obj) {
        AbstractOperation operation = OperationRegistry.reconstruct(project, obj);
        if (operation != null) {
            try {
                Process process = operation.createProcess(project, new Properties());
                
                project.processManager.queueProcess(process);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
