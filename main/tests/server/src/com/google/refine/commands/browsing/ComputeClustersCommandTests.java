
package com.google.refine.commands.browsing;

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
    public void testUserDefinedClustering() throws ServletException, IOException {
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

        JsonNode results = ParsingUtilities.mapper.readTree(writer.toString());
        assertEquals(results.get(0).size(), 3);
    }
}
