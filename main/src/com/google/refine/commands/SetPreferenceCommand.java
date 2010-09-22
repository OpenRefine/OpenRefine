package com.google.refine.commands;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONTokener;

import com.google.refine.ProjectManager;
import com.google.refine.model.Project;
import com.google.refine.preference.PreferenceStore;

public class SetPreferenceCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        Project project = request.getParameter("project") != null ? getProject(request) : null;
        PreferenceStore ps = project != null ? 
                project.getMetadata().getPreferenceStore() : 
                ProjectManager.singleton.getPreferenceStore();
                
        String prefName = request.getParameter("name");
        String valueString = request.getParameter("value");
        
        try {
            Object o = valueString == null ? null : new JSONTokener(valueString).nextValue();
            
            ps.put(prefName, PreferenceStore.loadObject(o));
            
            respond(response, "{ \"code\" : \"ok\" }");
        } catch (JSONException e) {
            respondException(response, e);
        }
    }

}
