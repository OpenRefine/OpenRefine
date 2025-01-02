
package com.google.refine.commands;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;

import javax.servlet.ServletException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.cell.BlankDownOperation;
import com.google.refine.operations.cell.FillDownOperation;
import com.google.refine.util.ParsingUtilities;

public class GetAllOperationsCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommandAndRegisterOperations() {
        command = new GetAllOperationsCommand();

        OperationRegistry.registerOperation(getCoreModule(), "blank-down", BlankDownOperation.class);
        OperationRegistry.registerOperation(getCoreModule(), "fill-down", FillDownOperation.class);
        OperationRegistry.registerOperationIcon("core/blank-down", "icons/blank-down.svg");
        OperationRegistry.registerOperationIcon("core/fill-down", null);
    }

    @Test
    public void testGet() throws ServletException, IOException {
        command.doGet(request, response);

        JsonNode root = ParsingUtilities.mapper.readTree(writer.toString());
        ArrayNode ops = (ArrayNode) root.get("operations");
        boolean blankDownSeen = false;
        boolean fillDownSeen = false;
        for (JsonNode entryNode : ops) {
            if ("core/blank-down".equals(entryNode.get("name").asText())) {
                blankDownSeen = true;
                assertEquals(entryNode.get("icon").asText(), "icons/blank-down.svg");
            } else if ("core/fill-down".equals(entryNode.get("name").asText())) {
                fillDownSeen = true;
                assertTrue(!entryNode.has("icon"));
            }
        }
        assertTrue(blankDownSeen, "blank down operation not listed");
        assertTrue(fillDownSeen, "fill down operation not listed");

    }
}
