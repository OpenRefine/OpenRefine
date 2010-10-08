package com.google.refine.freebase.commands;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine;
import com.google.refine.commands.Command;
import com.google.refine.freebase.ProtographTransposeExporter.TripleLoaderExporter;
import com.google.refine.freebase.util.FreebaseUtils;
import com.google.refine.model.Project;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.util.ParsingUtilities;

public class UploadDataCommand extends Command {
    final static public String s_dataLoadJobIDPref = "freebase.load.jobID";
    final static public String s_dataLoadJobNamePref = "freebase.load.jobName";
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        ProjectManager.singleton.setBusy(true);
        try {
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            PreferenceStore preferenceStore = project.getMetadata().getPreferenceStore();
            
            TripleLoaderExporter exporter = new TripleLoaderExporter();
            StringWriter triples = new StringWriter(10 * 1024 * 1024);
            exporter.export(project, new Properties(), engine, triples);

            String source_name = request.getParameter("source_name");
            String source_id = request.getParameter("source_id");
            String qa = request.getParameter("qa");
            String mdo_id = null;
            
            preferenceStore.put(s_dataLoadJobNamePref, source_name);
            
            try {
                Integer jobID = (Integer) preferenceStore.get(s_dataLoadJobIDPref);
                if (jobID != null) {
                    URL url = new URL("http://refinery.freebaseapps.com/job_id_to_mdo?job=" + jobID);
                    String s = ParsingUtilities.inputStreamToString(url.openConnection().getInputStream());
                    
                    if (!s.equals("null")) {
                        mdo_id = s;
                    }
                }
            } catch (Exception e) {
                // ignore
            }
            
            String uploadResponse = FreebaseUtils.uploadTriples(
                request, qa, source_name, source_id, mdo_id, triples.toString()
            );
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            try {
                JSONObject obj = new JSONObject(uploadResponse);
                if (obj.has("result") && !obj.isNull("result")) {
                    JSONObject result = obj.getJSONObject("result");
                    if (result.has("job_id") && !result.isNull("job_id")) {
                        Integer jobID = result.getInt("job_id");
                        project.getMetadata().getPreferenceStore().put(s_dataLoadJobIDPref, jobID);
                    }
                }
                response.getWriter().write(uploadResponse);
            } catch (JSONException e) {
                respond(response,"500 Error", uploadResponse);
            }
        } catch (Exception e) {
            respondException(response, e);
        } finally {
            ProjectManager.singleton.setBusy(false);
        }
    }
}
