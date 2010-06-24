package com.metaweb.gridworks.commands;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONTokener;

import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.preference.PreferenceStore;

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
            JSONTokener t = new JSONTokener(valueString);
            Object o = t.nextValue();
            
            ps.put(prefName, PreferenceStore.loadObject(o));
            
            respond(response, "{ \"code\" : \"ok\" }");
        } catch (JSONException e) {
            respondException(response, e);
        }
    }

}
