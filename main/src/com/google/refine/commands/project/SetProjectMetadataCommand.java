package com.google.refine.commands.project;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import com.google.refine.model.metadata.ProjectMetadata;

public class SetProjectMetadataCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Project project = request.getParameter("project") != null ? getProject(request) : null;
        String metaName = request.getParameter("name");
        String valueString = request.getParameter("value");
        ProjectMetadata meta = null;
        
        if (project == null) {
            respond(response, "{ \"code\" : \"error\", \"message\" : \"Project cannot be found\" }");
            return;
        }
        
        meta = project.getMetadata(); 
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            meta.setAnyStringField(metaName, valueString);
            ProjectManager.singleton.saveMetadata(meta, project.id);
            
            respond(response, "{ \"code\" : \"ok\" }");
        } catch (JSONException e) {
            respondException(response, e);
        } catch (Exception e) {
            respondException(response, e);
        }
    }

}
