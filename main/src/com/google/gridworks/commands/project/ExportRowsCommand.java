package com.google.gridworks.commands.project;

 import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gridworks.ProjectManager;
import com.google.gridworks.browsing.Engine;
import com.google.gridworks.commands.Command;
import com.google.gridworks.exporters.CsvExporter;
import com.google.gridworks.exporters.Exporter;
import com.google.gridworks.exporters.HtmlTableExporter;
import com.google.gridworks.exporters.TemplatingExporter;
import com.google.gridworks.exporters.XlsExporter;
import com.google.gridworks.exporters.ProtographTransposeExporter.MqlwriteLikeExporter;
import com.google.gridworks.exporters.ProtographTransposeExporter.TripleLoaderExporter;
import com.google.gridworks.model.Project;

public class ExportRowsCommand extends Command {

    static final protected Map<String, Exporter> s_formatToExporter = new HashMap<String, Exporter>();

    static {
        s_formatToExporter.put("html", new HtmlTableExporter());
        s_formatToExporter.put("xls", new XlsExporter());
        s_formatToExporter.put("csv", new CsvExporter());
        
        s_formatToExporter.put("template", new TemplatingExporter());
        
        s_formatToExporter.put("tripleloader", new TripleLoaderExporter());
        s_formatToExporter.put("mqlwrite", new MqlwriteLikeExporter());
    }
    
    static public void registerExporter(String format, Exporter exporter) {
        s_formatToExporter.put(format, exporter);
    }
    
    @SuppressWarnings("unchecked")
	static public Properties getRequestParameters(HttpServletRequest request) {
        Properties options = new Properties();
        
        Enumeration<String> en = request.getParameterNames();
        while (en.hasMoreElements()) {
        	String name = en.nextElement();
        	options.put(name, request.getParameter(name));
        }
    	return options;
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        ProjectManager.singleton.setBusy(true);
        try {
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            String format = request.getParameter("format");
            Properties options = getRequestParameters(request);

            Exporter exporter = s_formatToExporter.get(format.toLowerCase());
            if (exporter == null){
                exporter = new CsvExporter('\t');
            }

            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", exporter.getContentType());

            if (exporter.takeWriter()) {
                PrintWriter writer = response.getWriter();
                exporter.export(project, options, engine, writer);
            } else {
                exporter.export(project, options, engine, response.getOutputStream());
            }
        } catch (Exception e) {
            respondException(response, e);
        } finally {
            ProjectManager.singleton.setBusy(false);
        }
    }
}
