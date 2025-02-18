
package com.google.refine.commands.history;

import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.commands.Command;
import com.google.refine.commands.CommandTestBase;
import com.google.refine.expr.MetaParser;
import com.google.refine.grel.Parser;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.cell.MassEditOperation;
import com.google.refine.util.TestUtils;

public class GetColumnDependenciesCommandTest extends CommandTestBase {

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
        command = new GetColumnDependenciesCommand();
        OperationRegistry.registerOperation(getCoreModule(), "mass-edit", MassEditOperation.class);
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
    public void testValidJSON() throws Exception {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("operations")).thenReturn(historyJSON);

        command.doPost(request, response);

        String response = writer.toString();
        TestUtils.assertEqualsAsJson(response, "{\"code\":\"ok\", \"dependencies\":[\"bar\"], \"newColumns\":[] }");
    }

}
