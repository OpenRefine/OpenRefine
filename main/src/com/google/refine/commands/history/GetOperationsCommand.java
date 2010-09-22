package com.google.refine.commands.history;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.commands.Command;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Project;

public class GetOperationsCommand extends Command {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        Project project = getProject(request);
        
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            Properties options = new Properties();
            JSONWriter writer = new JSONWriter(response.getWriter());
            
            writer.object();
            writer.key("entries"); writer.array();
            
            for (HistoryEntry entry : project.history.getLastPastEntries(-1)) {
                writer.object();
                writer.key("description"); writer.value(entry.description);
                if (entry.operation != null) {
                    writer.key("operation");
                    entry.operation.write(writer, options);
                }
                writer.endObject();
            }
            writer.endArray();
            writer.endObject();
        } catch (JSONException e) {
            respondException(response, e);
        }
    }

}
