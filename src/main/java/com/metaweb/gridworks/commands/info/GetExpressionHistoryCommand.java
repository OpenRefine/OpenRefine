package com.metaweb.gridworks.commands.info;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.ProjectMetadata;
import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.model.Project;

public class GetExpressionHistoryCommand extends Command {
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            ProjectMetadata pm = ProjectManager.singleton.getProjectMetadata(project.id);
            
            List<String> localExpressions = pm.getExpressions();
            List<String> globalExpressions = ProjectManager.singleton.getExpressions();
            Set<String> done = new HashSet<String>();
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            JSONWriter writer = new JSONWriter(response.getWriter());
            writer.object();
            writer.key("expressions");
                writer.array();
                for (String s : localExpressions) {
                    writer.object();
                    writer.key("code"); writer.value(s);
                    writer.key("global"); writer.value(false);
                    writer.endObject();
                    done.add(s);
                }
                for (String s : globalExpressions) {
                    if (!done.contains(s)) {
                        writer.object();
                        writer.key("code"); writer.value(s);
                        writer.key("global"); writer.value(true);
                        writer.endObject();
                    }
                }
                writer.endArray();
            writer.endObject();
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
