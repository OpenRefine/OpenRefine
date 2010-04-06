package com.metaweb.gridworks.commands.edit;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.operations.ColumnRenameOperation;
import com.metaweb.gridworks.process.Process;

public class RenameColumnCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            
            String oldColumnName = request.getParameter("oldColumnName");
            String newColumnName = request.getParameter("newColumnName");
            
            AbstractOperation op = new ColumnRenameOperation(oldColumnName, newColumnName);
            Process process = op.createProcess(project, new Properties());
            
            performProcessAndRespond(request, response, project, process);
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
