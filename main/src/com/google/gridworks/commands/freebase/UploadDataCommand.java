package com.google.gridworks.commands.freebase;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gridworks.ProjectManager;
import com.google.gridworks.browsing.Engine;
import com.google.gridworks.commands.Command;
import com.google.gridworks.exporters.ProtographTransposeExporter.TripleLoaderExporter;
import com.google.gridworks.model.Project;
import com.google.gridworks.util.FreebaseUtils;
import com.google.gridworks.util.ParsingUtilities;

public class UploadDataCommand extends Command {
    final static public String s_dataLoadJobIDPref = "core/freebaseDataLoadJobID";
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        ProjectManager.singleton.setBusy(true);
        try {
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            TripleLoaderExporter exporter = new TripleLoaderExporter();
            StringWriter triples = new StringWriter(10 * 1024 * 1024);
            exporter.export(project, new Properties(), engine, triples);

            String source_name = request.getParameter("source_name");
            String source_id = request.getParameter("source_id");
            String graph = request.getParameter("graph");
            String mdo_id = null;
            
            try {
                Integer jobID = (Integer) project.getMetadata().getPreferenceStore().get(s_dataLoadJobIDPref);
                if (jobID != null) {
                    URL url = new URL("http://gridworks-loads.freebaseapps.com/job_id_to_mdo?job=" + jobID);
                    String s = ParsingUtilities.inputStreamToString(url.openConnection().getInputStream());
                    
                    if (!s.equals("null")) {
                        mdo_id = s;
                    }
                }
            } catch (Exception e) {
                // ignore
            }
            
            String uploadResponse = FreebaseUtils.uploadTriples(
                request, graph, source_name, source_id, mdo_id, triples.toString());
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            try {
                JSONObject obj = new JSONObject(uploadResponse);
                if (obj.has("result") && !obj.isNull("result")) {
                    JSONObject result = obj.getJSONObject("result");
                    if (result.has("job_id") && !result.isNull("job_id")) {
                        Integer jobID = result.getInt("job_id");
                        
                        project.getMetadata().getPreferenceStore().put(
                            s_dataLoadJobIDPref, jobID);
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
