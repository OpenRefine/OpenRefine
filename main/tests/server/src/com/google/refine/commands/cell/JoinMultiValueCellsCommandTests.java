
package com.google.refine.commands.cell;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

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

public class JoinMultiValueCellsCommandTests extends CommandTestBase {

    Project project;

    @BeforeMethod
    public void setUpCommandAndProject() {
        command = new JoinMultiValueCellsCommand();
        project = createProject(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { 1, 2 },
                        { null, 3 },
                });
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testMissingColumnName() throws ServletException, IOException {
        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        command.doPost(request, response);

        JsonNode node = ParsingUtilities.mapper.readValue(writer.toString(), JsonNode.class);
        assertEquals(node.get("code").asText(), "error");
        assertTrue(node.get("message").asText().contains("Missing column name"));
    }
}
