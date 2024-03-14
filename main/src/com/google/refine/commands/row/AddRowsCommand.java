
package com.google.refine.commands.row;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.google.refine.browsing.EngineConfig;
import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.operations.row.RowAdditionOperation;
import com.google.refine.util.Pool;

// It's not technically an engine dependent command, but it is easier to write
// by extending EngineDependentCommand than Command
public class AddRowsCommand extends EngineDependentCommand {

    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request,
            EngineConfig engineConfig) throws ServletException {

        String rowDataParam = "rows";
        if (!request.getParameterMap().containsKey(rowDataParam) || request.getParameter(rowDataParam) == null) {
            throw new ServletException(String.format("Parameter \"%s\" is required", rowDataParam));
        }

        // TODO: How would I use an existing pool?
        Pool pool = new Pool();
        String[] rowData = request.getParameterValues(rowDataParam);
        List<Row> rows = Arrays.stream(rowData)
                .map(rowStr -> {
                    try {
                        return Row.load(rowStr, pool);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

        return new RowAdditionOperation(rows);

    }

}
