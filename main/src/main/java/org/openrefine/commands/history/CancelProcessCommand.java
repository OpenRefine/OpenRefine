
package org.openrefine.commands.history;

import org.openrefine.commands.Command;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrefine.model.Project;
import org.openrefine.process.Process;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Cancels a single long-running process.
 */
public class CancelProcessCommand extends Command {

    protected class Response {

        @JsonProperty("code")
        public String code = "ok";
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        String processId = request.getParameter("id");

        try {
            Project project = getProject(request);

            try {
                int processIdInt = Integer.parseInt(processId);
                Process process = project.getProcessManager().getProcess(processIdInt);
                process.cancel();
            } catch (IllegalArgumentException e) {
                respondException(response, e);
            }
            respondJSON(response, new Response());
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
