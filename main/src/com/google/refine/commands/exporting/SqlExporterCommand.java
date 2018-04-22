package com.google.refine.commands.exporting;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.exporters.sql.SqlDataError;
import com.google.refine.exporters.sql.SqlExporter;
import com.google.refine.model.Project;


public class SqlExporterCommand extends Command {
    
    private static final Logger logger = LoggerFactory.getLogger("SqlExporterCommand");
    
     @SuppressWarnings("unchecked")
    public Properties getRequestParameters(HttpServletRequest request) {
        Properties options = new Properties();
        Enumeration<String> en = request.getParameterNames();
        while (en.hasMoreElements()) {
            String name = en.nextElement();
            String value = request.getParameter(name);
            options.put(name, value);
           
        }
        return options;
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        ProjectManager.singleton.setBusy(true);
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            JSONWriter writer = new JSONWriter(response.getWriter());
            writer.object();
            Project project = getProject(request);
            //Engine engine = getEngine(request, project);
            Properties params = getRequestParameters(request);
            SqlExporter sqlExporter = new SqlExporter();
            List<SqlDataError> result = sqlExporter.validateSqlData(project, params);
            if(result == null || result.isEmpty()) {
                writer.key("code");
                writer.value("OK");
            }else {
                writer.key("code");
                writer.value("Error");
                
                writer.key("SqlDataErrors"); 
                writer.array();
                result.stream().forEach(err -> {
                    writer.object();
                    
                    writer.key("errorcode");
                    writer.value(err.getErrorCode());
                    writer.key("errormessage");
                    writer.value(err.getErrorMessage());
                    
                    writer.endObject();
                    
                });
                writer.endArray();
                
            }
            
            writer.endObject();
           
        } catch (Exception e) {
          throw new ServletException(e);
        } finally {
            ProjectManager.singleton.setBusy(false);
        }
    }
  
}
