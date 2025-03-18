
package com.google.refine.commands.column;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

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
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class RenameColumnCommandTests extends CommandTestBase {

    String engineConfigJson = "{"
            + "\"mode\":\"row-based\","
            + "\"facets\":["
            + "   {"
            + "     \"selectNumeric\":true,"
            + "     \"expression\":\"cells[value].value\","
            + "     \"selectBlank\":false,"
            + "     \"selectNonNumeric\":true,"
            + "     \"selectError\":true,"
            + "     \"name\":\"some name\","
            + "     \"from\":13,"
            + "     \"to\":101,"
            + "     \"type\":\"range\","
            + "     \"columnName\":\"column A\""
            + "   },"
            + "   {"
            + "     \"selectNonTime\":true,"
            + "     \"expression\":\"grel:toDate(cells[\\\"column A\\\"].value)\","
            + "     \"selectBlank\":true,"
            + "     \"selectError\":true,"
            + "     \"selectTime\":true,"
            + "     \"name\":\"column A\","
            + "     \"from\":410242968000,"
            + "     \"to\":1262309184000,"
            + "     \"type\":\"timerange\","
            + "     \"columnName\":\"other_column\""
            + "   }"
            + "]}";

    String expectedConfigJson = "{"
            + "\"mode\":\"row-based\","
            + "\"facets\":["
            + "   {"
            + "     \"selectNumeric\":true,"
            + "     \"expression\":\"grel:cells.get(value).value\","
            + "     \"selectBlank\":false,"
            + "     \"selectNonNumeric\":true,"
            + "     \"selectError\":true,"
            + "     \"name\":\"some name\","
            + "     \"from\":13,"
            + "     \"to\":101,"
            + "     \"type\":\"range\","
            + "     \"columnName\":\"new_name\""
            + "   },"
            + "   {"
            + "     \"selectNonTime\":true,"
            + "     \"expression\":\"grel:toDate(cells.get(\\\"new_name\\\").value)\","
            + "     \"selectBlank\":true,"
            + "     \"selectError\":true,"
            + "     \"selectTime\":true,"
            + "     \"name\":\"column A\","
            + "     \"from\":410242968000,"
            + "     \"to\":1262309184000,"
            + "     \"type\":\"timerange\","
            + "     \"columnName\":\"other_column\""
            + "   }"
            + "]}";

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

    @Test
    public void testRenameWithEngine() throws Exception {
        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
        when(request.getParameter("oldColumnName")).thenReturn("column A");
        when(request.getParameter("newColumnName")).thenReturn("new_name");
        when(request.getParameter("engine")).thenReturn(engineConfigJson);
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        command.doPost(request, response);

        String response = writer.toString();
        JsonNode node = ParsingUtilities.mapper.readValue(response, JsonNode.class);
        assertEquals(node.get("code").asText(), "ok");
        TestUtils.isSerializedTo(node.get("newEngineConfig"), expectedConfigJson);

        Project expectedProject = createProject(new String[] { "new_name", "column B" },
                new Serializable[][] {
                        { 1, 2 },
                        { 3, 4 },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testMissingParameter() throws Exception {
        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
        when(request.getParameter("oldColumnName")).thenReturn("column A");
        when(request.getParameter("engine")).thenReturn(engineConfigJson);
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        command.doPost(request, response);

        String response = writer.toString();
        JsonNode node = ParsingUtilities.mapper.readValue(response, JsonNode.class);
        assertEquals(node.get("code").asText(), "error");
    }

    @Test
    public void testInvalidEngine() throws Exception {
        when(request.getParameter("new_column_name")).thenReturn("from_year");
        when(request.getParameter("oldColumnName")).thenReturn("column A");
        when(request.getParameter("newColumnName")).thenReturn("new_name");
        when(request.getParameter("engine")).thenReturn("{invalid_json");
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        command.doPost(request, response);

        String response = writer.toString();
        JsonNode node = ParsingUtilities.mapper.readValue(response, JsonNode.class);
        assertEquals(node.get("code").asText(), "error");
    }
}
