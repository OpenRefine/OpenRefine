package com.metaweb.gridworks.commands.info;

 import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.exporters.Exporter;
import com.metaweb.gridworks.exporters.TripleloaderExporter;
import com.metaweb.gridworks.exporters.TsvExporter;
import com.metaweb.gridworks.model.Project;

public class ExportRowsCommand extends Command {

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            String format = request.getParameter("format");
            
            PrintWriter writer = response.getWriter();
            
            Exporter exporter = null;
            if ("tripleloader".equalsIgnoreCase(format)) {
                exporter = new TripleloaderExporter();
            } else {
                exporter = new TsvExporter();
            }
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", exporter.getContentType());
            
            exporter.export(project, new Properties(), engine, writer);

        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
