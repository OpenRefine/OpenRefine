package com.metaweb.gridworks.commands.edit;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.changes.RowFlagChange;
import com.metaweb.gridworks.model.changes.RowStarChange;
import com.metaweb.gridworks.process.QuickHistoryEntryProcess;

public class AnnotateOneRowCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        
        try {
            Project project = getProject(request);
            
            int rowIndex = Integer.parseInt(request.getParameter("row"));
            
            String starredString = request.getParameter("starred");
            if (starredString != null) {
                boolean starred = "true".endsWith(starredString);
                String description = (starred ? "Star row " : "Unstar row ") + (rowIndex + 1); 

                StarOneRowProcess process = new StarOneRowProcess(
                    project, 
                    description,
                    rowIndex, 
                    starred
                );
                
                performProcessAndRespond(request, response, project, process);
                return;
            }

            String flaggedString = request.getParameter("flagged");
            if (flaggedString != null) {
                boolean flagged = "true".endsWith(flaggedString);
                String description = (flagged ? "Flag row " : "Unflag row ") + (rowIndex + 1); 

                FlagOneRowProcess process = new FlagOneRowProcess(
                    project, 
                    description,
                    rowIndex, 
                    flagged
                );
                
                performProcessAndRespond(request, response, project, process);
                return;
            }

            respond(response, "{ \"code\" : \"error\", \"message\" : \"invalid command parameters\" }");
            
        } catch (Exception e) {
            respondException(response, e);
        }
    }
    
    protected static class StarOneRowProcess extends QuickHistoryEntryProcess {
        final int rowIndex;
        final boolean starred;
        
        StarOneRowProcess(
            Project project, 
            String briefDescription, 
            int rowIndex, 
            boolean starred
        ) {
            super(project, briefDescription);
            
            this.rowIndex = rowIndex;
            this.starred = starred;
        }

        protected HistoryEntry createHistoryEntry(long historyEntryID) throws Exception {
            return new HistoryEntry(
                historyEntryID,
                _project, 
                (starred ? "Star row " : "Unstar row ") + (rowIndex + 1), 
                null, 
                new RowStarChange(rowIndex, starred)
            );
        }
    }
    protected static class FlagOneRowProcess extends QuickHistoryEntryProcess {
        final int rowIndex;
        final boolean flagged;
        
        FlagOneRowProcess(
            Project project, 
            String briefDescription, 
            int rowIndex, 
            boolean flagged
        ) {
            super(project, briefDescription);
            
            this.rowIndex = rowIndex;
            this.flagged = flagged;
        }

        protected HistoryEntry createHistoryEntry(long historyEntryID) throws Exception {
            return new HistoryEntry(
                historyEntryID,
                _project, 
                (flagged ? "Flag row " : "Unflag row ") + (rowIndex + 1), 
                null, 
                new RowFlagChange(rowIndex, flagged)
            );
        }
    }
}
