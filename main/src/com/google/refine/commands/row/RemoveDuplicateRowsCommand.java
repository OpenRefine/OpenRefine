package com.google.refine.commands.row;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.type.TypeReference;

import com.google.refine.commands.Command;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.row.RowDuplicatesRemovalOperation;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;

public class RemoveDuplicateRowsCommand extends Command {

/*    @Override
    protected AbstractOperation createOperation(Project project,
                                                HttpServletRequest request, EngineConfig engineConfig) throws Exception {

        String duplicateCriteria = request.getParameter("criteria");
        return new RowDuplicatesRemovalOperation(ParsingUtilities.mapper.readValue(duplicateCriteria, new TypeReference<List<String>>() {
        }), engineConfig);
    }*/

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        try {
            Project project = getProject(request);
            String duplicateCriteria = request.getParameter("criteria");

            AbstractOperation op = new RowDuplicatesRemovalOperation(ParsingUtilities.mapper.readValue(duplicateCriteria, new TypeReference<List<String>>() {
            }));
            Process process = op.createProcess(project, new Properties());

            performProcessAndRespond(request, response, project, process);
        } catch (Exception e) {
            respondException(response, e);
        }
    }

}
