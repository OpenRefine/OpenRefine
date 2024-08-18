
package com.google.refine.commands.project;

import static com.google.refine.util.TestUtils.assertEqualsAsJson;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.ServletException;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectManagerStub;
import com.google.refine.ProjectMetadata;
import com.google.refine.commands.Command;
import com.google.refine.commands.CommandTestBase;
import com.google.refine.model.Project;
import com.google.refine.model.ProjectStub;
import com.google.refine.util.ParsingUtilities;

public class SetProjectTagsCommandTests extends CommandTestBase {

    private static long PROJECT_ID = 1234;
    private String[] TAGS = new String[] { "tag1", "tag2" };
    ProjectManager projectManager = null;
    Project project = null;
    PrintWriter pw = null;

    @BeforeMethod
    public void setUpCommand() throws IOException {
        command = new SetProjectTagsCommand();

        pw = new PrintWriter(writer);

        ProjectMetadata metadata = new ProjectMetadata();
        metadata.setUserMetadata((ArrayNode) ParsingUtilities.mapper.readTree("[ {name: \"clientID\", display: true} ]"));
        metadata.setTags(TAGS);

        projectManager = new ProjectManagerStub();
        ProjectManager.singleton = projectManager;
        project = new ProjectStub(PROJECT_ID);
        ProjectManager.singleton.registerProject(project, metadata);

        // mock dependencies
        when(request.getParameter("project")).thenReturn(Long.toString(PROJECT_ID));
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        when(response.getWriter()).thenReturn(pw);
    }

    @Test
    public void setProjectTagsTest() throws ServletException, IOException {
        // Check that our tags got set up correctly at project registration time.
        assertEquals(ProjectManager.singleton.getAllProjectsTags().keySet().stream().sorted().toArray(String[]::new), TAGS);

        when(request.getParameter("old")).thenReturn(String.join(",", TAGS));
        when(request.getParameter("new")).thenReturn("a b c");
        // run
        command.doPost(request, response);

        // verify
        verify(request, times(1)).getParameter("csrf_token");
        verify(request, times(1)).getParameter("project");

        verify(response, times(1))
                .setHeader("Content-Type", "application/json");
        verify(response, times(1)).getWriter();
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
    public void testCSRFProtection() throws ServletException, IOException {
        when(request.getParameter("csrf_token")).thenReturn(null);
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }
}
