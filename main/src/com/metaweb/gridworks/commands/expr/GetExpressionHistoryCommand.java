package com.metaweb.gridworks.commands.expr;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.preference.TopList;

public class GetExpressionHistoryCommand extends Command {
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            
            List<String> localExpressions = 
                ((TopList) project.getMetadata().getPreferenceStore().get("expressions"))
                    .getList();
            
            localExpressions = localExpressions.size() > 20 ? localExpressions.subList(0, 20) : localExpressions;
            
            List<String> globalExpressions =
                ((TopList) ProjectManager.singleton.getPreferenceStore().get("expressions"))
                    .getList();
            
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
