
package org.openrefine.commands.history;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.Serializable;

import org.openrefine.commands.Command;
import org.openrefine.commands.CommandTestBase;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.model.Project;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.cell.MassEditOperation;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    public void testCSRFProtection() throws Exception {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testEmptyOperations() throws Exception {
        String operationsJson = "[]";
        when(request.getParameter("operations")).thenReturn(operationsJson);
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("project")).thenReturn(Long.toString(project.getId()));

        command.doPost(request, response);

        verify(response).setStatus(202);
        String jsonResponse = writer.toString();
        TestUtils.assertEqualsAsJson(jsonResponse, "{\"code\":\"ok\", \"results\":[] }");
    }

    @Test(expectedExceptions = JsonParseException.class)
    public void testInvalidJson() throws Exception {
        String operationsJson = "[invalid_json";
        when(request.getParameter("operations")).thenReturn(operationsJson);
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("project")).thenReturn(Long.toString(project.getId()));

        command.doPost(request, response);
    }

    @Test
    public void testSuccessfullyApplyOneOperation() throws Exception {
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

        verify(response).setStatus(202);
        ObjectNode jsonResponse = (ObjectNode) ParsingUtilities.mapper.readTree(writer.toString());
        // we cannot directly check the JSON for equality with a known object because it contains a timestamp and unique
        // id
        assertEquals(jsonResponse.get("code").asText(), "ok");
        assertEquals(((ArrayNode) jsonResponse.get("results")).size(), 1);
        assertTrue(((ArrayNode) jsonResponse.get("results")).get(0).get("success").asBoolean());
        assertEquals(((ArrayNode) jsonResponse.get("results")).get(0).get("historyEntry").get("description").asText(),
                "Mass edit cells in column bar");
    }

    @Test
    public void testMismatchingOperation() throws Exception {
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

        verify(response).setStatus(400);
        ObjectNode jsonResponse = (ObjectNode) ParsingUtilities.mapper.readTree(writer.toString());
        // we cannot directly check the JSON for equality with a known object because it contains a timestamp and unique
        // id
        assertEquals(jsonResponse.get("code").asText(), "error");
        assertEquals(((ArrayNode) jsonResponse.get("results")).size(), 1);
        assertFalse(((ArrayNode) jsonResponse.get("results")).get(0).get("success").asBoolean());
        assertEquals(((ArrayNode) jsonResponse.get("results")).get(0).get("error").get("message").asText(),
                "Column 'non_existent_column' does not exist.");
    }
}
