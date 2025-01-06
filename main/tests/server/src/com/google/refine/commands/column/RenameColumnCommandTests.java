
package com.google.refine.commands.column;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;

import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.commands.Command;
import com.google.refine.commands.CommandTestBase;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

public class RenameColumnCommandTests extends CommandTestBase {

    Project project;

    @BeforeMethod
    public void setUpCommandAndProject() {
        command = new RenameColumnCommand();

        project = createProject(new String[] { "column A", "column B" },
                new Serializable[][] {
                        { 1, 2 },
                        { 3, 4 },
                });
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testSimpleRename() throws Exception {
        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
        when(request.getParameter("oldColumnName")).thenReturn("column A");
        when(request.getParameter("newColumnName")).thenReturn("new_name");
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        command.doPost(request, response);

        String response = writer.toString();
        JsonNode node = ParsingUtilities.mapper.readValue(response, JsonNode.class);
        assertEquals(node.get("code").asText(), "ok");

        Project expectedProject = createProject(new String[] { "new_name", "column B" },
                new Serializable[][] {
                        { 1, 2 },
                        { 3, 4 },
                });
        assertProjectEquals(project, expectedProject);
    }
}
