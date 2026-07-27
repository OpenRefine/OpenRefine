
package com.google.refine.commands.browsing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

public class ComputeClustersCommandTests extends CommandTestBase {

    Project project;

    @BeforeMethod
    public void setUpCommand() {
        command = new ComputeClustersCommand();
        project = createProject(new String[] { "foo", "bar " },
                new Serializable[][] {
                        { "ecole", "1" },
                        { "école", "3" },
                        { "École", "2" },
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
    public void testUserDefinedClustering() throws Exception {
        String clusteringConf = "{"
                + "  \"type\": \"binning\","
                + "  \"params\":{"
                + "    \"expression\": \"value.fingerprint()\""
                + "  },"
                + "  \"function\": \"UserDefinedKeyer\""
                + "}";
        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("clusterer")).thenReturn(clusteringConf);

        command.doPost(request, response);

        // Command now returns async "pending" response
        JsonNode result = ParsingUtilities.mapper.readTree(writer.toString());
        assertEquals(result.get("code").asText(), "pending");

        // Wait for async process to complete
        long deadline = System.currentTimeMillis() + 5000;
        while (ComputeClustersCommand.getActiveProcess(project.id) != null
                && !ComputeClustersCommand.getActiveProcess(project.id).isCompleted()
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }

        // Poll status via GetClusteringStatusCommand
        GetClusteringStatusCommand statusCmd = new GetClusteringStatusCommand();
        HttpServletRequest statusRequest = mock(HttpServletRequest.class);
        HttpServletResponse statusResponse = mock(HttpServletResponse.class);
        StringWriter statusWriter = new StringWriter();
        when(statusResponse.getWriter()).thenReturn(new PrintWriter(statusWriter));
        when(statusRequest.getParameter("project")).thenReturn(Long.toString(project.id));

        statusCmd.doGet(statusRequest, statusResponse);

        JsonNode statusResult = ParsingUtilities.mapper.readTree(statusWriter.toString());
        assertEquals(statusResult.get("status").asText(), "done");
        assertEquals(statusResult.get("clusters").get(0).size(), 3);
    }
}
