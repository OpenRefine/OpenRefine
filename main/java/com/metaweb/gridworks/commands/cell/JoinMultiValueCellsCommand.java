package com.metaweb.gridworks.commands.cell;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.operations.cell.MultiValuedCellJoinOperation;
import com.metaweb.gridworks.process.Process;

public class JoinMultiValueCellsCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            
            String columnName = request.getParameter("columnName");
            String keyColumnName = request.getParameter("keyColumnName");
            String separator = request.getParameter("separator");
            
            AbstractOperation op = new MultiValuedCellJoinOperation(columnName, keyColumnName, separator);
            Process process = op.createProcess(project, new Properties());
            
            performProcessAndRespond(request, response, project, process);
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
