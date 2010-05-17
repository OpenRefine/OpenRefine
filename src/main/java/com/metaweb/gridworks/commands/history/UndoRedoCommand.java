package com.metaweb.gridworks.commands.history;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.history.HistoryProcess;
import com.metaweb.gridworks.model.Project;

public class UndoRedoCommand extends Command {
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        Project project = getProject(request);
        
        long lastDoneID = -1;
        String lastDoneIDString = request.getParameter("lastDoneID");
        if (lastDoneIDString != null) {
            lastDoneID = Long.parseLong(lastDoneIDString);
        } else {
            String undoIDString = request.getParameter("undoID");
            if (undoIDString != null) {
                long undoID = Long.parseLong(undoIDString);
                
                lastDoneID = project.history.getPrecedingEntryID(undoID);
            }
        }
        
        boolean done = lastDoneID == -1 ||
            project.processManager.queueProcess(
                new HistoryProcess(project, lastDoneID));

        respond(response, "{ \"code\" : " + (done ? "\"ok\"" : "\"pending\"") + " }");
    }
}
