package com.google.gridworks.commands.project;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;

import com.google.gridworks.ProjectManager;
import com.google.gridworks.commands.Command;
import com.google.gridworks.model.Project;

public class GetProjectMetadataCommand extends Command {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        Project project = getProject(request);
        
        try {
            respondJSON(response, ProjectManager.singleton.getProjectMetadata(project.id));
        } catch (JSONException e) {
            respondException(response, e);
        }
    }
}
