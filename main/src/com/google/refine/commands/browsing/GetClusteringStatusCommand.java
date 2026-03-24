
package com.google.refine.commands.browsing;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.google.refine.clustering.ClusteringProcess;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

public class GetClusteringStatusCommand extends Command {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Project project = getProject(request);
            ClusteringProcess process = ComputeClustersCommand.getActiveProcess(project.id);

            if (process == null) {
                ObjectNode result = ParsingUtilities.mapper.createObjectNode();
                result.put("status", "idle");
                respondJSON(response, result);
                return;
            }

            process.updateProgress();

            if (process.isFailed()) {
                ObjectNode result = ParsingUtilities.mapper.createObjectNode();
                result.put("status", "error");
                respondJSON(response, result);
            } else if (process.isCompleted()) {
                respondJSON(response, ParsingUtilities.mapper.createObjectNode()
                        .put("status", "done")
                        .set("clusters", ParsingUtilities.mapper.valueToTree(process.getClusterer())));
            } else if (process.isRunning()) {
                ObjectNode result = ParsingUtilities.mapper.createObjectNode();
                result.put("status", "running");
                result.put("progress", process.getClusterer().getProgress());
                respondJSON(response, result);
            } else {
                // Pending (queued but not started)
                ObjectNode result = ParsingUtilities.mapper.createObjectNode();
                result.put("status", "pending");
                result.put("progress", 0);
                respondJSON(response, result);
            }
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
