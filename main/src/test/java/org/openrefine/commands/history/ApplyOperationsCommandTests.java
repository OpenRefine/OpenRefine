
package org.openrefine.commands.history;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testng.annotations.*;

import org.openrefine.commands.Command;
import org.openrefine.commands.CommandTestBase;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.model.Project;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.cell.MassEditOperation;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ApplyOperationsCommandTests extends CommandTestBase {

    Project project;

    @BeforeMethod
    public void setUpDependencies() {
        command = new ApplyOperationsCommand();
        OperationRegistry.registerOperation("core", "mass-edit", MassEditOperation.class);
        project = createProject("test project",
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "hello", "world" },
                        { null, 123 },
                });
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
    }

    @AfterMethod
    public void tearDownDependencies() {
        project = null;
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testEmptyOperations() throws ServletException, IOException {
        String operationsJson = "[]";
        when(request.getParameter("operations")).thenReturn(operationsJson);
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("project")).thenReturn(Long.toString(project.getId()));

        command.doPost(request, response);

        String jsonResponse = writer.toString();

        String expectedResponse = "{\"code\":\"ok\", \"results\":[] }";
        TestUtils.assertEqualsAsJson(jsonResponse, expectedResponse);
    }

    @Test
    public void testInvalidJson() throws ServletException, IOException {
        String operationsJson = "[invalid_json";
        when(request.getParameter("operations")).thenReturn(operationsJson);
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("project")).thenReturn(Long.toString(project.getId()));

        command.doPost(request, response);

        ObjectNode jsonResponse = (ObjectNode) ParsingUtilities.mapper.readTree(writer.toString());
        // we cannot directly check the JSON for equality with a known object because it contains a timestamp and unique
        // id
        assertEquals(jsonResponse.get("code").asText(), "error");
    }

    @Test
    public void testSuccessfullyApplyOneOperation() throws ServletException, IOException {
        String operationsJson = "[" +
                "  {\n" +
                "    \"op\": \"core/mass-edit\",\n" +
                "    \"engineConfig\": {\n" +
                "      \"facets\": [],\n" +
                "      \"mode\": \"row-based\",\n" +
                "      \"aggregationLimit\": 1000\n" +
                "    },\n" +
                "    \"columnName\": \"bar\",\n" +
                "    \"expression\": \"value\",\n" +
                "    \"edits\": [\n" +
                "      {\n" +
                "        \"from\": [\n" +
                "          \"world\"\n" +
                "        ],\n" +
                "        \"fromBlank\": false,\n" +
                "        \"fromError\": false,\n" +
                "        \"to\": \"monde\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"description\": \"Mass edit cells in column bar\"\n" +
                "  }" +
                "]";
        when(request.getParameter("operations")).thenReturn(operationsJson);
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("project")).thenReturn(Long.toString(project.getId()));

        command.doPost(request, response);

        ObjectNode jsonResponse = (ObjectNode) ParsingUtilities.mapper.readTree(writer.toString());
        // we cannot directly check the JSON for equality with a known object because it contains a timestamp and unique
        // id
        assertEquals(jsonResponse.get("code").asText(), "ok");
        assertEquals(((ArrayNode) jsonResponse.get("results")).size(), 1);
        assertEquals(((ArrayNode) jsonResponse.get("results")).get(0).get("status").asText(), "applied");
        assertEquals(((ArrayNode) jsonResponse.get("results")).get(0).get("historyEntry").get("description").asText(),
                "Mass edit cells in column bar");
    }

    @Test
    public void testMismatchingOperation() throws ServletException, IOException {
        String operationsJson = "[" +
                "  {\n" +
                "    \"op\": \"core/mass-edit\",\n" +
                "    \"engineConfig\": {\n" +
                "      \"facets\": [],\n" +
                "      \"mode\": \"row-based\",\n" +
                "      \"aggregationLimit\": 1000\n" +
                "    },\n" +
                "    \"columnName\": \"non_existent_column\",\n" +
                "    \"expression\": \"value\",\n" +
                "    \"edits\": [\n" +
                "      {\n" +
                "        \"from\": [\n" +
                "          \"world\"\n" +
                "        ],\n" +
                "        \"fromBlank\": false,\n" +
                "        \"fromError\": false,\n" +
                "        \"to\": \"monde\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"description\": \"Mass edit cells in column non_existent_column\"\n" +
                "  }" +
                "]";
        when(request.getParameter("operations")).thenReturn(operationsJson);
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("project")).thenReturn(Long.toString(project.getId()));

        command.doPost(request, response);

        ObjectNode jsonResponse = (ObjectNode) ParsingUtilities.mapper.readTree(writer.toString());
        // we cannot directly check the JSON for equality with a known object because it contains a timestamp and unique
        // id
        assertEquals(jsonResponse.get("code").asText(), "error");
        assertEquals(((ArrayNode) jsonResponse.get("results")).size(), 1);
        assertEquals(((ArrayNode) jsonResponse.get("results")).get(0).get("status").asText(), "failed");
        assertEquals(((ArrayNode) jsonResponse.get("results")).get(0).get("errorMessage").asText(),
                "Applying the operation failed: Column 'non_existent_column' does not exist");
    }
}
