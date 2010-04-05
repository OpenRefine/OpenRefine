package com.metaweb.gridworks.commands.info;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.ProjectMetadata;
import com.metaweb.gridworks.commands.Command;

public class GetAllProjectMetadataCommand extends Command {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            JSONWriter writer = new JSONWriter(response.getWriter());
            Properties options = new Properties();
            
            writer.object();
            
            writer.key("projects");
                writer.object();
                Map<Long, ProjectMetadata> m = ProjectManager.singleton.getAllProjectMetadata();
                for (Entry<Long,ProjectMetadata> e : m.entrySet()) {
                    writer.key(e.getKey().toString());
                    e.getValue().write(writer, options);
                }
                writer.endObject();
                
            writer.endObject();
        } catch (JSONException e) {
            respondException(response, e);
        }
    }
}