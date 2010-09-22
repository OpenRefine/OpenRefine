package com.google.refine.commands;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.ProjectManager;
import com.google.refine.model.Project;
import com.google.refine.preference.PreferenceStore;

public class GetAllPreferencesCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        Project project = request.getParameter("project") != null ? getProject(request) : null;
        PreferenceStore ps = project != null ? 
                project.getMetadata().getPreferenceStore() : 
                ProjectManager.singleton.getPreferenceStore();
                
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            JSONWriter writer = new JSONWriter(response.getWriter());
            
            writer.object();
            
            for (String key : ps.getKeys()) {
                Object pref = ps.get(key);
                if (pref == null || pref instanceof String || pref instanceof Number || pref instanceof Boolean) {
                    writer.key(key);
                    writer.value(pref);
                }
            }
            
            writer.endObject();
        } catch (JSONException e) {
            respondException(response, e);
        }
    }

}
