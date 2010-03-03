package com.metaweb.gridworks.commands.edit;

  import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.operations.MultiValuedCellSplitOperation;
import com.metaweb.gridworks.process.Process;

public class SplitMultiValueCellsCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            
            String columnName = request.getParameter("columnName");
            String keyColumnName = request.getParameter("keyColumnName");
            String separator = request.getParameter("separator");
            String mode = request.getParameter("mode");
            
            AbstractOperation op = new MultiValuedCellSplitOperation(columnName, keyColumnName, separator, mode);
            Process process = op.createProcess(project, new Properties());
            
            boolean done = project.processManager.queueProcess(process);
            
            respond(response, "{ \"code\" : " + (done ? "\"ok\"" : "\"pending\"") + " }");
            
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
