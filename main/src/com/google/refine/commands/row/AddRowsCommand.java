
package com.google.refine.commands.row;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.operations.row.RowAdditionOperation;
import com.google.refine.process.Process;
import com.google.refine.util.Pool;

public class AddRowsCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        try {
            Project project = getProject(request);

            String rowDataParam = "rows[]";
            if (!request.getParameterMap().containsKey(rowDataParam) || request.getParameter(rowDataParam) == null) {
                throw new ServletException(String.format("Parameter \"%s\" is required", rowDataParam));
            }
            String[] rowData = request.getParameterValues(rowDataParam);

            // TODO: load Rows with existing pool. Current implementation requires users to re-reconcile values
            //   in a previously reconciled column.
            Pool pool = new Pool();

            List<Row> rows = Arrays.stream(rowData)
                    .map(rowStr -> {
                        try {
                            return Row.load(rowStr, pool);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

            AbstractOperation op = new RowAdditionOperation(rows);
            Process process = op.createProcess(project, new Properties());

            performProcessAndRespond(request, response, project, process);
        } catch (Exception e) {
            respondException(response, e);
        }
    }

//    @Override
//    protected AbstractOperation createOperation(Project project,
//            HttpServletRequest request,
//            EngineConfig engineConfig) throws ServletException {
//

//

//
//        return
//
//    }

}
