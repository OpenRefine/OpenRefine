package com.metaweb.gridworks.commands.project;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.model.Project;

public class GetModelsCommand extends Command {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        Project project = getProject(request);
        
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            Properties options = new Properties();
            JSONWriter writer = new JSONWriter(response.getWriter());
            
            writer.object();
            writer.key("columnModel"); project.columnModel.write(writer, options);
            writer.key("protograph"); 
            if (project.protograph == null) {
                writer.value(null);
            } else {
                project.protograph.write(writer, options);
            }
            writer.endObject();
        } catch (JSONException e) {
            respondException(response, e);
        }
    }

}
