
package org.openrefine.commands.history;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.commands.Command;
import org.openrefine.commands.CommandTestBase;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.grel.Parser;
import org.openrefine.model.Project;
import org.openrefine.model.changes.Change;
import org.openrefine.operations.OnError;
import org.openrefine.operations.cell.FillDownOperation;
import org.openrefine.operations.cell.TextTransformOperation;
import org.openrefine.util.ParsingUtilities;

public class UndoRedoCommandTests extends CommandTestBase {

    Project project;

    @BeforeMethod
    public void setUpCommand() throws Change.DoesNotApplyException, ParsingException {
        command = new UndoRedoCommand();
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");

        // set up a project with two changes
        project = createProject("my project",
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "test", 23L },
                        { null, 42L },
                        { "record", "banana" },
                        { null, "apple" }
                });
        // the first one preserves records
        TextTransformOperation transform = new TextTransformOperation(
                EngineConfig.ALL_ROWS,
                "foo",
                "grel:value.toString() + \"_concat\"",
                OnError.StoreError,
                false,
                0);
        project.getHistory().addEntry(1234L, transform);
        // the second one only rows
        FillDownOperation fillDown = new FillDownOperation(EngineConfig.ALL_RECORDS, "foo");
        project.getHistory().addEntry(5678L, fillDown);
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testLastDoneId() throws ServletException, IOException {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("project")).thenReturn(Long.toString(project.getId()));
        when(request.getParameter("lastDoneID")).thenReturn("1234");

        command.doPost(request, response);

        ObjectNode jsonResponse = (ObjectNode) ParsingUtilities.mapper.readTree(writer.toString());
        Assert.assertEquals(jsonResponse.get("code").asText(), "ok");
        Assert.assertEquals(jsonResponse.get("gridPreservation").asText(), "preserves-rows");
    }

    @Test
    public void testUndoId() throws ServletException, IOException {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("project")).thenReturn(Long.toString(project.getId()));
        when(request.getParameter("undoID")).thenReturn("5678");

        command.doPost(request, response);

        ObjectNode jsonResponse = (ObjectNode) ParsingUtilities.mapper.readTree(writer.toString());
        Assert.assertEquals(jsonResponse.get("code").asText(), "ok");
        Assert.assertEquals(jsonResponse.get("gridPreservation").asText(), "preserves-rows");
    }
}
