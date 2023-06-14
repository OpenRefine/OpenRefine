
package org.openrefine.commands.history;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrefine.commands.Command;
import org.openrefine.model.Project;
import org.openrefine.process.Process;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A command to resume a process fetching some change data.
 */
public class ResumeProcessCommand extends Command {

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

        Project project = getProject(request);

        int processIdInt = Integer.parseInt(processId);
        Process process = project.getProcessManager().getProcess(processIdInt);
        process.resume();

        respondJSON(response, 202, new Response());
    }
}
