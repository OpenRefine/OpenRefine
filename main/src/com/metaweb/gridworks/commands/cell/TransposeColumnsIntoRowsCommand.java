package com.metaweb.gridworks.commands.cell;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.operations.cell.TransposeColumnsIntoRowsOperation;
import com.metaweb.gridworks.process.Process;

public class TransposeColumnsIntoRowsCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            
            String startColumnName = request.getParameter("startColumnName");
            int columnCount = Integer.parseInt(request.getParameter("columnCount"));
            String combinedColumnName = request.getParameter("combinedColumnName");
            
            boolean prependColumnName = Boolean.parseBoolean(request.getParameter("prependColumnName"));
            String separator = request.getParameter("separator");
            boolean ignoreBlankCells = Boolean.parseBoolean(request.getParameter("ignoreBlankCells"));
            
            AbstractOperation op = new TransposeColumnsIntoRowsOperation(
                    startColumnName, columnCount, combinedColumnName, prependColumnName, separator, ignoreBlankCells);
            
            Process process = op.createProcess(project, new Properties());
            
            performProcessAndRespond(request, response, project, process);
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
