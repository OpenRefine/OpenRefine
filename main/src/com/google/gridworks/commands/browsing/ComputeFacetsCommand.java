package com.google.gridworks.commands.browsing;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gridworks.browsing.Engine;
import com.google.gridworks.commands.Command;
import com.google.gridworks.model.Project;

public class ComputeFacetsCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            
            engine.computeFacets();
            
            respondJSON(response, engine);
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
