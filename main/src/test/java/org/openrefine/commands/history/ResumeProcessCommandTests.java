
package org.openrefine.commands.history;

import static org.mockito.Mockito.*;
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
import org.openrefine.model.Grid;
import org.openrefine.model.Project;
import org.openrefine.process.Process;
import org.openrefine.process.ProcessManager;
import org.openrefine.util.TestUtils;

public class ResumeProcessCommandTests extends CommandTestBase {

    long projectId = 1234L;
    int processId = 5678;
    int missingProcessId = 9876;
    Project project;
    Grid grid;
    ProjectMetadata projectMetadata;
    ProcessManager processManager;
    Process process;

    @BeforeMethod
    public void setUpCommand() {
        command = new ResumeProcessCommand();
        project = mock(Project.class);
        when(project.getId()).thenReturn(projectId);
        grid = mock(Grid.class);
        when(project.getCurrentGrid()).thenReturn(grid);
        projectMetadata = mock(ProjectMetadata.class);
        when(projectMetadata.getTags()).thenReturn(new String[] {});
        Instant now = Instant.now();
        when(projectMetadata.getModified()).thenReturn(now);
        when(project.getLastSave()).thenReturn(now);
        processManager = mock(ProcessManager.class);
        when(project.getProcessManager()).thenReturn(processManager);
        process = mock(Process.class);
        when(processManager.getProcess(processId)).thenReturn(process);
        when(processManager.getProcess(missingProcessId)).thenThrow(new IllegalArgumentException("process not found"));

        ProjectManager.singleton.registerProject(project, projectMetadata);
    }

    @Test
    public void testCSRFProtection() throws Exception {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testSuccessfulResume() throws Exception {
        when(request.getParameter("project")).thenReturn(Long.toString(projectId));
        when(request.getParameter("id")).thenReturn(Integer.toString(processId));
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        command.doPost(request, response);

        verify(response).setStatus(202);
        verify(process, times(1)).resume();
        TestUtils.assertEqualsAsJson(writer.toString(), "{\"code\":\"ok\"}");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testProcessNotFound() throws Exception {
        when(request.getParameter("project")).thenReturn(Long.toString(projectId));
        when(request.getParameter("id")).thenReturn(Integer.toString(missingProcessId));
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        command.doPost(request, response);
    }
}
