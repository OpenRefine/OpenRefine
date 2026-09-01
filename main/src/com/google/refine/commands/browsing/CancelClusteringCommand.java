
package com.google.refine.commands.browsing;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

public class CancelClusteringCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        try {
            Project project = getProject(request);
            ComputeClustersCommand.cancelActiveProcess(project.id);
            respondJSON(response, ParsingUtilities.mapper.createObjectNode().put("code", "ok"));
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
