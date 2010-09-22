package com.google.refine.commands.expr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import com.google.refine.preference.TopList;

public class GetExpressionHistoryCommand extends Command {
    
	static protected List<String> toExpressionList(Object o) {
        return o == null ? new ArrayList<String>() : ((TopList) o).getList();
	}

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            
            List<String> localExpressions = toExpressionList(project.getMetadata().getPreferenceStore().get("scripting.expressions"));
            localExpressions = localExpressions.size() > 20 ? localExpressions.subList(0, 20) : localExpressions;
            
            List<String> globalExpressions = toExpressionList(ProjectManager.singleton.getPreferenceStore().get("scripting.expressions"));
            
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
