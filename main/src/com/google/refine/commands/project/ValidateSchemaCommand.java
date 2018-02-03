package com.google.refine.commands.project;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import com.google.refine.model.medadata.validator.ValidateOperation;
import com.google.refine.util.ParsingUtilities;

public class ValidateSchemaCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        ProjectManager.singleton.setBusy(true);
        try {
            Project project = getProject(request);
            JSONObject optionObj = ParsingUtilities.evaluateJsonStringToObject(
                    request.getParameter("options"));
            
            new ValidateOperation(project, optionObj).startProcess();
            
            respond(response, "{ \"code\" : \"ok\" }");
        } catch (JSONException e) {
            respondException(response, e);
        } catch (ServletException e) {
            respond(response, "error", e.getLocalizedMessage());
            return;
        } finally {
            ProjectManager.singleton.setBusy(false);
        }
    }
}
