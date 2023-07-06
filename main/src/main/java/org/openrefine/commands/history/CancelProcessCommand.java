
package org.openrefine.commands.history;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.commands.Command;
import org.openrefine.history.History;
import org.openrefine.model.Project;
import org.openrefine.process.Process;
import org.openrefine.process.ProcessManager;

/**
 * Cancels a single long-running process.
 */
public class CancelProcessCommand extends Command {

    protected class Response {

        @JsonProperty("code")
        public String code = "ok";

        /**
         * if the position in the history changed, this holds the new history entry id. Otherwise, it is null.
         */
        @JsonProperty("newHistoryEntryId")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Long newHistoryEntryId = null;

        public Response(Long historyEntryId) {
            newHistoryEntryId = historyEntryId;
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        String processId = request.getParameter("id");

        Project project = getProject(request);
        Long newHistoryEntryId = null;

        int processIdInt = Integer.parseInt(processId);
        ProcessManager processManager = project.getProcessManager();
        Process process = processManager.getProcess(processIdInt);
        long historyEntryId = process.getChangeDataId().getHistoryEntryId();
        History history = project.getHistory();
        if (history.getPosition() >= history.entryIndex(historyEntryId) + 1) {
            long precedingEntryID = history.getPrecedingEntryID(historyEntryId);
            newHistoryEntryId = Math.max(precedingEntryID, 0L);
            history.undoRedo(newHistoryEntryId);
        }
        process.cancel();
        processManager.update();
        respondJSON(response, 202, new Response(newHistoryEntryId));
    }
}
