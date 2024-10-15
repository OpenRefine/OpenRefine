
package com.google.refine.commands.history;

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
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.cell.MassEditOperation;
import com.google.refine.util.ParsingUtilities;

public class ApplyOperationsCommandTests extends CommandTestBase {

    Project project;
    String historyJSON = "[\n"
            + "  {\n"
            + "    \"op\": \"core/mass-edit\",\n"
            + "    \"engineConfig\": {\n"
            + "      \"facets\": [],\n"
            + "      \"mode\": \"row-based\"\n"
            + "    },\n"
            + "    \"columnName\": \"bar\",\n"
            + "    \"expression\": \"value\",\n"
            + "    \"edits\": [\n"
            + "      {\n"
            + "        \"from\": [\n"
            + "          \"hello\"\n"
            + "        ],\n"
            + "        \"fromBlank\": false,\n"
            + "        \"fromError\": false,\n"
            + "        \"to\": \"hallo\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"description\": \"Mass edit cells in column bar\"\n"
            + "  }\n"
            + "]";

    @BeforeMethod
    public void setUpDependencies() {
        command = new ApplyOperationsCommand();
        project = createProject(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { 1, "hello" },
                        { null, true }
                });
        OperationRegistry.registerOperation(getCoreModule(), "mass-edit", MassEditOperation.class);
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testInvalidProject() throws Exception {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("project")).thenReturn(String.format("%d9", project.id));
        when(request.getParameter("operations")).thenReturn(historyJSON);

        command.doPost(request, response);

        String response = writer.toString();
        JsonNode node = ParsingUtilities.mapper.readValue(response, JsonNode.class);
        assertEquals(node.get("code").toString(), "\"error\"");
    }

    @Test
    public void testInvalidJSON() throws Exception {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("project")).thenReturn(project.toString());
        when(request.getParameter("operations")).thenReturn("[{}]");

        command.doPost(request, response);

        String response = writer.toString();
        JsonNode node = ParsingUtilities.mapper.readValue(response, JsonNode.class);
        assertEquals(node.get("code").toString(), "\"error\"");
    }
}
