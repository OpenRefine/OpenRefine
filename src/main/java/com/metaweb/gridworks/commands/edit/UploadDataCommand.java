package com.metaweb.gridworks.commands.edit;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.exporters.TripleloaderExporter;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.util.FreebaseUtils;

public class UploadDataCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
    	ProjectManager.singleton.setBusy(true);
        try {
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            TripleloaderExporter exporter = new TripleloaderExporter();
            StringWriter triples = new StringWriter(10 * 1024 * 1024);
            exporter.export(project, new Properties(), engine, triples);

            String source_name = request.getParameter("source_name");
            String source_id = request.getParameter("source_id");
            String graph = request.getParameter("graph");
                        
            String result = FreebaseUtils.uploadTriples(request, graph, source_name, source_id, triples.toString());

            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            try {
                new JSONObject(result);
                response.getWriter().write(result);
            } catch (JSONException e) {
                respond(response,"500 Error", result);
            }
            
        } catch (Exception e) {
            respondException(response, e);
        } finally {
        	ProjectManager.singleton.setBusy(false);
        }
    }
}
