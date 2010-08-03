package com.google.gridworks.commands.history;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gridworks.commands.Command;
import com.google.gridworks.history.HistoryProcess;
import com.google.gridworks.model.Project;

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
        
        try {
            boolean done = lastDoneID == -1 ||
                project.processManager.queueProcess(
                    new HistoryProcess(project, lastDoneID));
            
            respond(response, "{ \"code\" : " + (done ? "\"ok\"" : "\"pending\"") + " }");
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
