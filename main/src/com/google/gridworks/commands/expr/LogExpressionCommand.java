package com.google.gridworks.commands.expr;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gridworks.ProjectManager;
import com.google.gridworks.commands.Command;
import com.google.gridworks.model.Project;
import com.google.gridworks.preference.TopList;

public class LogExpressionCommand extends Command {
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            String expression = request.getParameter("expression");
            
            ((TopList) project.getMetadata().getPreferenceStore().get("scripting.expressions"))
                .add(expression);

            ((TopList) ProjectManager.singleton.getPreferenceStore().get("scripting.expressions"))
                .add(expression);
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            response.getWriter().write("{ \"code\" : \"ok\" }");
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
