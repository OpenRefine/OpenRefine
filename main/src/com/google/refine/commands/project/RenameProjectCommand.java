package com.google.refine.commands.project;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.ProjectMetadata;
import com.google.refine.commands.Command;

public class RenameProjectCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            String name = request.getParameter("name");
            ProjectMetadata pm = getProjectMetadata(request);
            
            pm.setName(name);
            
            respond(response, "{ \"code\" : \"ok\" }");
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
