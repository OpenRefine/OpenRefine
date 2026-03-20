
package com.google.refine.commands.workspace;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;

import com.fasterxml.jackson.databind.JsonNode;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.commands.CommandTestBase;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.util.ParsingUtilities;

public class GetProjectsCommandTests extends CommandTestBase {

    ProjectManager projMan = null;

    @BeforeMethod
    public void setUp() {
        projMan = mock(ProjectManager.class);
        ProjectManager.singleton = projMan;

        PreferenceStore preferenceStore = new PreferenceStore();
        when(projMan.getPreferenceStore()).thenReturn(preferenceStore);

        command = new GetProjectsCommand();
    }

    @AfterMethod
    public void tearDown() {
        projMan = null;
        ProjectManager.singleton = null;
    }

    private Map<Long, ProjectMetadata> makeProjects(int count) {
        Map<Long, ProjectMetadata> projects = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            long id = 1000L + i;
            ProjectMetadata meta = new ProjectMetadata(
                    Instant.parse("2023-01-01T00:00:00Z"),
                    Instant.parse("2023-06-01T00:00:00Z"),
                    "Project " + i);
            projects.put(id, meta);
        }
        return projects;
    }

    @Test
    public void testDefaultPagination() throws ServletException, IOException {
        Map<Long, ProjectMetadata> projects = makeProjects(60);
        when(projMan.getAllProjectMetadata()).thenReturn(projects);

        command.doGet(request, response);

        JsonNode result = ParsingUtilities.mapper.readTree(writer.toString());
        Assert.assertEquals(result.get("total").asInt(), 60);
        Assert.assertEquals(result.get("start").asInt(), 0);
        Assert.assertEquals(result.get("limit").asInt(), 50);
        Assert.assertEquals(result.get("projects").size(), 50);
    }

    @Test
    public void testCustomLimit() throws ServletException, IOException {
        Map<Long, ProjectMetadata> projects = makeProjects(10);
        when(projMan.getAllProjectMetadata()).thenReturn(projects);
        when(request.getParameter("limit")).thenReturn("5");

        command.doGet(request, response);

        JsonNode result = ParsingUtilities.mapper.readTree(writer.toString());
        Assert.assertEquals(result.get("total").asInt(), 10);
        Assert.assertEquals(result.get("limit").asInt(), 5);
        Assert.assertEquals(result.get("projects").size(), 5);
    }

    @Test
    public void testPaginationWithStart() throws ServletException, IOException {
        Map<Long, ProjectMetadata> projects = makeProjects(10);
        when(projMan.getAllProjectMetadata()).thenReturn(projects);
        when(request.getParameter("start")).thenReturn("7");
        when(request.getParameter("limit")).thenReturn("5");

        command.doGet(request, response);

        JsonNode result = ParsingUtilities.mapper.readTree(writer.toString());
        Assert.assertEquals(result.get("total").asInt(), 10);
        Assert.assertEquals(result.get("start").asInt(), 7);
        Assert.assertEquals(result.get("projects").size(), 3);
    }

    @Test
    public void testProjectIdIncluded() throws ServletException, IOException {
        Map<Long, ProjectMetadata> projects = makeProjects(1);
        when(projMan.getAllProjectMetadata()).thenReturn(projects);

        command.doGet(request, response);

        JsonNode result = ParsingUtilities.mapper.readTree(writer.toString());
        JsonNode firstProject = result.get("projects").get(0);
        Assert.assertTrue(firstProject.has("id"), "Project object should include 'id' field");
        Assert.assertEquals(firstProject.get("id").asLong(), 1000L);
        Assert.assertTrue(firstProject.has("name"), "Project object should include 'name' field");
    }

    @Test
    public void testEmptyProjects() throws ServletException, IOException {
        when(projMan.getAllProjectMetadata()).thenReturn(new LinkedHashMap<>());

        command.doGet(request, response);

        JsonNode result = ParsingUtilities.mapper.readTree(writer.toString());
        Assert.assertEquals(result.get("total").asInt(), 0);
        Assert.assertEquals(result.get("projects").size(), 0);
    }
}
