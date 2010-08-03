package com.google.gridworks.rdf.commands;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gridworks.commands.Command;
import com.google.gridworks.model.Project;
import com.google.gridworks.rdf.Util;

public class SaveBaseURI extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Project project = getProject(request);
            String base = request.getParameter("baseURI");
            
            Util.getProjectSchema(project).setBaseUri(base);
            
            project.getMetadata().updateModified();
            
            respond(response,"OK","");
            
        }catch(Exception e){
            respondException(response, e);
        }
    }
}
