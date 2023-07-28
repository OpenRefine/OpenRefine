
package org.openrefine.commands.lang;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.openrefine.RefineServlet;
import org.openrefine.commands.CommandTestBase;
import org.openrefine.io.FileProjectManager;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.mit.simile.butterfly.ButterflyModule;

public class LoadLanguageCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() throws IOException {
        FileProjectManager.initialize(runner(), TestUtils.createTempDirectory("openrefine-test-workspace-dir"));
        command = new LoadLanguageCommand();
        ButterflyModule coreModule = mock(ButterflyModule.class);

        when(coreModule.getName()).thenReturn("core");
        when(coreModule.getPath()).thenReturn(new File("webapp/modules/core"));
        RefineServlet servlet = mock(RefineServlet.class);
        when(servlet.getModule("core")).thenReturn(coreModule);
        command.init(servlet);
    }

    @Test
    public void testLoadSingleLanguage() throws Exception {
        when(request.getParameter("module")).thenReturn("core");
        when(request.getParameterValues("lang")).thenReturn(new String[] { "en_GB" });

        command.doPost(request, response);

        verify(response).setStatus(200);
        JsonNode response = ParsingUtilities.mapper.readValue(writer.toString(), JsonNode.class);
        assertTrue(response.has("dictionary"));
        assertEquals(response.get("lang").asText(), "en_GB");
    }

    @Test
    public void testLoadMultiLanguages() throws Exception {
        when(request.getParameter("module")).thenReturn("core");
        when(request.getParameterValues("lang")).thenReturn(new String[] { "ja", "it", "es", "de" });

        command.doPost(request, response);

        verify(response).setStatus(200);
        JsonNode response = ParsingUtilities.mapper.readValue(writer.toString(), JsonNode.class);
        assertTrue(response.has("dictionary"));
        assertEquals(response.get("lang").asText(), "ja");
    }

    @Test
    public void testLoadUnknownLanguage() throws Exception {
        when(request.getParameter("module")).thenReturn("core");
        when(request.getParameterValues("lang")).thenReturn(new String[] { "foobar" });

        command.doPost(request, response);

        verify(response).setStatus(200);
        JsonNode response = ParsingUtilities.mapper.readValue(writer.toString(), JsonNode.class);
        assertTrue(response.has("dictionary"));
        assertEquals(response.get("lang").asText(), "en");
    }

    @Test
    public void testLoadNoLanguage() throws Exception {
        when(request.getParameter("module")).thenReturn("core");
        when(request.getParameterValues("lang")).thenReturn(new String[] { "" });

        command.doPost(request, response);

        verify(response).setStatus(200);
        JsonNode response = ParsingUtilities.mapper.readValue(writer.toString(), JsonNode.class);
        assertTrue(response.has("dictionary"));
        assertEquals(response.get("lang").asText(), "en");
    }

    @Test
    public void testLoadNullLanguage() throws Exception {
        when(request.getParameter("module")).thenReturn("core");
        when(request.getParameterValues("lang")).thenReturn(null);

        command.doPost(request, response);

        verify(response).setStatus(200);
        JsonNode response = ParsingUtilities.mapper.readValue(writer.toString(), JsonNode.class);
        assertTrue(response.has("dictionary"));
        assertEquals(response.get("lang").asText(), "en");
    }

    @Test
    public void testLanguageFallback() throws Exception {
        String fallbackJson = "{"
                + "\"foo\":\"hello\","
                + "\"bar\":\"world\""
                + "}";
        String preferredJson = "{"
                + "\"foo\":\"hallo\""
                + "}";
        String expectedJson = "{"
                + "\"foo\":\"hallo\","
                + "\"bar\":\"world\""
                + "}";
        ObjectNode fallback = ParsingUtilities.mapper.readValue(fallbackJson, ObjectNode.class);
        ObjectNode preferred = ParsingUtilities.mapper.readValue(preferredJson, ObjectNode.class);
        ObjectNode expected = ParsingUtilities.mapper.readValue(expectedJson, ObjectNode.class);

        ObjectNode merged = LoadLanguageCommand.mergeLanguages(preferred, fallback);

        assertEquals(merged, expected);
    }
}