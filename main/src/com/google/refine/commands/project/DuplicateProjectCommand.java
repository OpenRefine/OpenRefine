package com.google.refine.commands.project;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.io.FileProjectManager;

public class DuplicateProjectCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        response.setHeader("Content-Type", "application/json");

        try {
            long projectID = Long.parseLong(request.getParameter("project"));
            FileProjectManager manager = (FileProjectManager) ProjectManager.singleton;
            manager.duplicateProject(projectID);

            respond(response, "{ \"code\" : \"ok\" }");

        } catch (Exception e) {
            respondException(response, e);
        }
    }

}
