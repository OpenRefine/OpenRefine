
package org.openrefine.commands.project;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openrefine.util.TestUtils.assertEqualsAsJson;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.ProjectManager;
import org.openrefine.ProjectManagerStub;
import org.openrefine.ProjectMetadata;
import org.openrefine.commands.Command;
import org.openrefine.commands.CommandTestBase;
import org.openrefine.history.History;
import org.openrefine.model.Grid;
import org.openrefine.model.Project;
import org.openrefine.util.ParsingUtilities;

public class SetProjectTagsCommandTests extends CommandTestBase {

    private static long PROJECT_ID = 1234;
    private String[] TAGS = new String[] { "tag1", "tag2" };
    ProjectManager projectManager = null;
    Project project = null;
    History history;
    Grid grid;
    PrintWriter pw = null;

    @BeforeMethod
    public void setUpCommand() throws JsonProcessingException {
        command = new SetProjectTagsCommand();

        pw = new PrintWriter(writer);

        ProjectMetadata metadata = new ProjectMetadata();
        metadata.setUserMetadata((ArrayNode) ParsingUtilities.mapper.readTree("[ {name: \"clientID\", display: true} ]"));
        metadata.setTags(TAGS);

        projectManager = new ProjectManagerStub(runner());
        ProjectManager.singleton = projectManager;
        history = mock(History.class);
        grid = mock(Grid.class);
        when(history.getCurrentGrid()).thenReturn(grid);
        when(grid.rowCount()).thenReturn(1000L);
        when(history.getLastModified()).thenReturn(Instant.now());
        project = new Project(PROJECT_ID, history);
        ProjectManager.singleton.registerProject(project, metadata);

        // mock dependencies
        when(request.getParameter("project")).thenReturn(Long.toString(PROJECT_ID));
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        try {
            when(response.getWriter()).thenReturn(pw);
        } catch (IOException e1) {
            Assert.fail();
        }
    }

    @Test
    public void setProjectTagsTest() {
        // Check that our tags got set up correctly at project registration time.
        assertEquals(ProjectManager.singleton.getAllProjectsTags().keySet().stream().sorted().toArray(String[]::new), TAGS);

        when(request.getParameter("old")).thenReturn(String.join(",", TAGS));
        when(request.getParameter("new")).thenReturn("a b c");
        // run
        try {
            command.doPost(request, response);
        } catch (Exception e) {
            Assert.fail();
        }

        // verify
        verify(request, times(1)).getParameter("csrf_token");
        verify(request, times(1)).getParameter("project");

        verify(response, times(1))
                .setHeader("Content-Type", "application/json");
        try {
            verify(response, times(1)).getWriter();
        } catch (IOException e) {
            Assert.fail();
        }
        assertEqualsAsJson(writer.toString(), "{\"code\":\"ok\"}");

        String[] tags = project.getMetadata().getTags();
        assertEquals(tags.length, 3);
        Arrays.sort(tags);
        assertEquals(tags[1], "b");

        Map<String, Integer> tagCounts = ProjectManager.singleton.getAllProjectsTags();
        assertEquals(tagCounts.size(), 3);
        assertEquals(ProjectManager.singleton.getAllProjectsTags().keySet().stream().sorted().toArray(String[]::new)[1], "b");

        projectManager.deleteProject(PROJECT_ID);
        // TODO: ProjectManagerStub doesn't manage tags on project delete, so this fails
//        assertEquals(ProjectManager.singleton.getAllProjectsTags().size(), 0);
    }

    @Test
    public void testCSRFProtection() throws Exception {
        when(request.getParameter("csrf_token")).thenReturn(null);
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

}
