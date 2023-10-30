package com.google.refine.commands.cell;

import com.google.refine.commands.Command;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.cell.MultiValuedCellZipOperation;
import com.google.refine.process.Process;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ZipColumnsCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        try {
            Project project = getProject(request);

            String column1 = request.getParameter("column1");
            String column2 = request.getParameter("column2");
            String targetColumn = request.getParameter("targetColumn");

            AbstractOperation op = new MultiValuedCellZipOperation(column1, column2, targetColumn); // 使用自定义的 Zip 操作
            Process process = op.createProcess(project, null);

            performProcessAndRespond(request, response, project, process);
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
