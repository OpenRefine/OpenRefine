
package org.openrefine.commands.project;

import static org.mockito.Mockito.mock;
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
import org.openrefine.util.TestUtils;

public class DeleteProjectCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() {
        command = new DeleteProjectCommand();
    }

    @Test
    public void testCSRFProtection() throws Exception {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testAcceptsNullTags() throws Exception {
        Project project = mock(Project.class);
        when(project.getId()).thenReturn(1234L);
        Instant now = Instant.now();
        when(project.getLastSave()).thenReturn(now);
        when(project.getCurrentGrid()).thenReturn(mock(Grid.class));
        when(project.getLastModified()).thenReturn(now);
        ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        when(projectMetadata.getModified()).thenReturn(now);

        ProjectManager.singleton.registerProject(project, projectMetadata);

        when(request.getParameter("project")).thenReturn("1234");
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        command.doPost(request, response);

        verify(response).setStatus(200);
        TestUtils.assertEqualsAsJson(writer.toString(), "{\"code\":\"ok\"}");
    }
}
