package com.google.refine.commands.expr;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.preference.TopList;


public class GetStarredExpressionsCommand extends Command {
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            List<String> starredExpressions = ((TopList)ProjectManager.singleton.getPreferenceStore().get("scripting.starred-expressions")).getList();
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            JSONWriter writer = new JSONWriter(response.getWriter());
            writer.object();
            writer.key("expressions");
                writer.array();
                for (String s : starredExpressions) {
                    writer.object();
                    writer.key("code"); writer.value(s);
                    writer.endObject();
                }
                writer.endArray();
            writer.endObject();
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
