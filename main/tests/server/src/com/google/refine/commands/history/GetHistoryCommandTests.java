
package com.google.refine.commands.history;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;

import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.browsing.EngineConfig;
import com.google.refine.commands.CommandTestBase;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.cell.FillDownOperation;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class GetHistoryCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() {
        command = new GetHistoryCommand();
    }

    @Test
    public void testEmptyHistory() throws ServletException, IOException {
        Project project = createProject(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { null, "c" },
                });
        when(request.getParameter("project")).thenReturn(String.format("%d", project.id));

        command.doGet(request, response);

        String response = writer.toString();
        TestUtils.assertEqualsAsJson(response, "{\"past\":[], \"future\":[]}");
    }

    @Test
    public void testOperationId() throws Exception {
        Project project = createProject(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { null, "c" },
                });
        AbstractOperation op = new FillDownOperation(EngineConfig.defaultRowBased(), "foo");
        OperationRegistry.registerOperation(getCoreModule(), "fill-down", FillDownOperation.class);
        runOperation(op, project);

        when(request.getParameter("project")).thenReturn(String.format("%d", project.id));

        command.doGet(request, response);

        String response = writer.toString();
        JsonNode node = ParsingUtilities.mapper.readValue(response, JsonNode.class);
        assertEquals(node.get("past").get(0).get("operation_id").asText(), "core/fill-down");
    }
}
