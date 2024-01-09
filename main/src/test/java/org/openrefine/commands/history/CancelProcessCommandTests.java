
package org.openrefine.commands.history;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.ProjectManager;
import org.openrefine.ProjectMetadata;
import org.openrefine.commands.Command;
import org.openrefine.commands.CommandTestBase;
import org.openrefine.history.History;
import org.openrefine.model.Grid;
import org.openrefine.model.Project;
import org.openrefine.model.changes.ChangeDataId;
import org.openrefine.process.Process;
import org.openrefine.process.ProcessManager;
import org.openrefine.util.TestUtils;

public class CancelProcessCommandTests extends CommandTestBase {

    long projectId = 1234L;
    int processId = 5678;
    int missingProcessId = 9876;
    Project project;
    Grid grid;
    ProjectMetadata projectMetadata;
    ProcessManager processManager;
    Process process;
    History history;

    @BeforeMethod
    public void setUpCommand() {
        command = new CancelProcessCommand();
        project = mock(Project.class);
        when(project.getId()).thenReturn(projectId);
        grid = mock(Grid.class);
        when(project.getCurrentGrid()).thenReturn(grid);
        projectMetadata = mock(ProjectMetadata.class);
        when(projectMetadata.getTags()).thenReturn(new String[] {});
        Instant now = Instant.now();
        when(projectMetadata.getModified()).thenReturn(now);
        when(project.getLastSave()).thenReturn(now);
        when(project.getLastModified()).thenReturn(now);
        processManager = mock(ProcessManager.class);
        when(project.getProcessManager()).thenReturn(processManager);
        process = mock(Process.class);
        when(process.getChangeDataId()).thenReturn(new ChangeDataId(1234L, "data"));
        when(processManager.getProcess(processId)).thenReturn(process);
        when(processManager.getProcess(missingProcessId)).thenThrow(new IllegalArgumentException("missing"));
        history = mock(History.class);
        when(project.getHistory()).thenReturn(history);
        when(history.entryIndex(1234L)).thenReturn(3);
        when(history.getPrecedingEntryID(1234L)).thenReturn(5678L);

        ProjectManager.singleton.registerProject(project, projectMetadata);
    }

    @Test
    public void testCSRFProtection() throws Exception {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testSuccessfulCancel() throws Exception {
        when(request.getParameter("project")).thenReturn(Long.toString(projectId));
        when(request.getParameter("id")).thenReturn(Integer.toString(processId));
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        when(history.getPosition()).thenReturn(1);

        command.doPost(request, response);

        verify(response).setStatus(202);
        verify(history, times(0)).undoRedo(5678L);
        verify(process, times(1)).cancel();
        TestUtils.assertEqualsAsJson(writer.toString(), "{\"code\":\"ok\"}");
    }

    @Test
    public void testCancelAndUndo() throws Exception {
        when(request.getParameter("project")).thenReturn(Long.toString(projectId));
        when(request.getParameter("id")).thenReturn(Integer.toString(processId));
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        when(history.getPosition()).thenReturn(5);

        command.doPost(request, response);

        verify(response).setStatus(202);
        verify(history, times(1)).undoRedo(5678L);
        verify(history, times(1)).deleteFutureEntries();
        verify(process, times(1)).cancel();
        TestUtils.assertEqualsAsJson(writer.toString(), "{\"code\":\"ok\",\"newHistoryEntryId\":5678}");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testProcessNotFound() throws Exception {
        when(request.getParameter("project")).thenReturn(Long.toString(projectId));
        when(request.getParameter("id")).thenReturn(Integer.toString(missingProcessId));
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        command.doPost(request, response);
    }
}
