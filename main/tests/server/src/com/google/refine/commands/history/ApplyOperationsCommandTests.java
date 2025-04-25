
package com.google.refine.commands.history;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;

import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.commands.Command;
import com.google.refine.commands.CommandTestBase;
import com.google.refine.expr.MetaParser;
import com.google.refine.grel.Parser;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationDescription;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.cell.MassEditOperation;
import com.google.refine.operations.cell.TextTransformOperation;
import com.google.refine.operations.column.ColumnRenameOperation;
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
        OperationRegistry.registerOperation(getCoreModule(), "column-rename", ColumnRenameOperation.class);
        OperationRegistry.registerOperation(getCoreModule(), "text-transform", TextTransformOperation.class);
    }

    @BeforeMethod
    public void registerGRELParser() {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
    }

    @AfterMethod
    public void unregisterGRELParser() {
        MetaParser.unregisterLanguageParser("grel");
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testHappyPath() throws Exception {
        String json = "[{\"op\":\"core/column-rename\","
                + "\"description\":\"Rename column foo to foo2\","
                + "\"oldColumnName\":\"foo\","
                + "\"newColumnName\":\"foo2\"}]";

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("project")).thenReturn(String.format("%d", project.id));
        when(request.getParameter("operations")).thenReturn(json);

        command.doPost(request, response);

        String response = writer.toString();
        JsonNode node = ParsingUtilities.mapper.readValue(response, JsonNode.class);
        assertEquals(node.get("code").asText(), "ok");
        assertEquals(node.get("historyEntries").get(0).get("description").asText(),
                OperationDescription.column_rename_brief("foo", "foo2"));
    }

    @Test
    public void testConflictingIntermediateColumn() throws Exception {
        String renamesJSON = "{"
                + "  \"a\": \"bar\""
                + "}";

        // recipe that contains a column that only exists during intermediate steps
        String historyWithTemporaryColumn = "[\n"
                + "  {\n"
                + "    \"op\": \"core/column-rename\",\n"
                + "    \"oldColumnName\": \"a\",\n"
                + "    \"newColumnName\": \"foo\",\n"
                + "    \"description\": \"Rename column a to foo\"\n"
                + "  },\n"
                + "  {\n"
                + "    \"op\": \"core/column-rename\",\n"
                + "    \"oldColumnName\": \"foo\",\n"
                + "    \"newColumnName\": \"a2\",\n"
                + "    \"description\": \"Rename column foo to a2\"\n"
                + "  }\n"
                + "]";

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("project")).thenReturn(String.format("%d", project.id));
        when(request.getParameter("operations")).thenReturn(historyWithTemporaryColumn);
        when(request.getParameter("renames")).thenReturn(renamesJSON);

        command.doPost(request, response);

        String response = writer.toString();
        JsonNode node = ParsingUtilities.mapper.readValue(response, JsonNode.class);
        assertEquals(node.get("code").asText(), "ok");
        assertEquals(node.get("historyEntries").get(0).get("description").asText(),
                OperationDescription.column_rename_brief("bar", "foo_2"));
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
        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
        when(request.getParameter("operations")).thenReturn("[{}]");

        command.doPost(request, response);

        String response = writer.toString();
        JsonNode node = ParsingUtilities.mapper.readValue(response, JsonNode.class);
        assertEquals(node.get("code").toString(), "\"error\"");
    }

    @Test
    public void testMissingColumnInJSON() throws Exception {
        String json = "[{\"op\":\"core/column-rename\","
                + "\"description\":\"Rename column old name to new name\","
                + "\"oldColumnName\":\"old name\","
                + "\"newColumnName\":\"new name\"}]";

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
        when(request.getParameter("operations")).thenReturn(json);

        command.doPost(request, response);

        String response = writer.toString();
        JsonNode node = ParsingUtilities.mapper.readValue(response, JsonNode.class);
        assertEquals(node.get("code").toString(), "\"error\"");
    }

    @Test
    public void testDuplicateNewColumn() throws Exception {
        String json = "[{\"op\":\"core/column-rename\","
                + "\"description\":\"Rename column foo to bar\","
                + "\"oldColumnName\":\"foo\","
                + "\"newColumnName\":\"bar\"}]";

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
        when(request.getParameter("operations")).thenReturn(json);

        command.doPost(request, response);

        String response = writer.toString();
        JsonNode node = ParsingUtilities.mapper.readValue(response, JsonNode.class);
        assertEquals(node.get("code").toString(), "\"error\"");
    }

    @Test
    public void testInvalidExpression() throws Exception {
        String json = "[{"
                + "   \"op\":\"core/text-transform\","
                + "   \"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
                + "   \"columnName\":\"foo\","
                + "   \"expression\":\"grel:\\\"invalid\","
                + "   \"onError\":\"set-to-blank\","
                + "   \"repeat\": false,"
                + "   \"repeatCount\": 0"
                + "}]";

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
        when(request.getParameter("operations")).thenReturn(json);

        command.doPost(request, response);

        String response = writer.toString();
        JsonNode node = ParsingUtilities.mapper.readValue(response, JsonNode.class);
        assertEquals(node.get("code").toString(), "\"error\"");
        assertEquals(node.get("operationIndex").asInt(), 0);
        assertTrue(node.get("message").asText().startsWith("Operation #1: Invalid expression"));
    }

    @Test
    public void testValidWithoutRenames() throws Exception {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
        when(request.getParameter("operations")).thenReturn(historyJSON);

        command.doPost(request, response);

        String response = writer.toString();
        JsonNode node = ParsingUtilities.mapper.readValue(response, JsonNode.class);
        assertEquals(node.get("code").asText(), "ok");

        Project expectedProject = createProject(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { 1, "hallo" },
                        { null, true }
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testValidWithRenames() throws Exception {
        String renamesJSON = "{"
                + "  \"bar\": \"bar_orig\""
                + "}";

        project = createProject(new String[] { "foo", "bar_orig" },
                new Serializable[][] {
                        { 1, "hello" },
                        { null, true }
                });

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
        when(request.getParameter("operations")).thenReturn(historyJSON);
        when(request.getParameter("renames")).thenReturn(renamesJSON);

        command.doPost(request, response);

        String response = writer.toString();
        JsonNode node = ParsingUtilities.mapper.readValue(response, JsonNode.class);
        assertEquals(node.get("code").asText(), "ok");

        Project expectedProject = createProject(new String[] { "foo", "bar_orig" },
                new Serializable[][] {
                        { 1, "hallo" },
                        { null, true }
                });
        assertProjectEquals(project, expectedProject);
    }

}
