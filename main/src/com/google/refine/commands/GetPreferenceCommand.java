package com.google.refine.commands;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.ProjectManager;
import com.google.refine.model.Project;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.preference.TopList;

public class GetPreferenceCommand extends Command {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        Project project = request.getParameter("project") != null ? getProject(request) : null;
        PreferenceStore ps = project != null ? 
                project.getMetadata().getPreferenceStore() : 
                ProjectManager.singleton.getPreferenceStore();
                
        String prefName = request.getParameter("name");
        Object pref = ps.get(prefName);
        
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            JSONWriter writer = new JSONWriter(response.getWriter());
            
            writer.object();
            writer.key("value");
            if (pref == null || pref instanceof String || pref instanceof Number || pref instanceof Boolean) {
                writer.value(pref);
            } else if (pref instanceof TopList) {
                TopList tl = (TopList) pref;
                tl.write(writer, new Properties());
            } else {
                writer.value(pref.toString());
            }
            
            writer.endObject();
        } catch (JSONException e) {
            respondException(response, e);
        }
    }

}
