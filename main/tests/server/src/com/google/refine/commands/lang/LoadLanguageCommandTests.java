
package com.google.refine.commands.lang;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.servlet.ServletException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.mit.simile.butterfly.ButterflyModule;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.RefineServlet;
import com.google.refine.commands.CommandTestBase;
import com.google.refine.io.FileProjectManager;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class LoadLanguageCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() throws IOException {
        FileProjectManager.initialize(TestUtils.createTempDirectory("openrefine-test-workspace-dir"));
        command = new LoadLanguageCommand();
        ButterflyModule coreModule = mock(ButterflyModule.class);

        when(coreModule.getName()).thenReturn("core");
        when(coreModule.getPath()).thenReturn(new File("webapp/modules/core"));
        RefineServlet servlet = mock(RefineServlet.class);
        when(servlet.getModule("core")).thenReturn(coreModule);
        command.init(servlet);
    }

    @Test
    public void testLoadSingleLanguage() throws ServletException, IOException {
        when(request.getParameter("module")).thenReturn("core");
        when(request.getParameterValues("lang")).thenReturn(new String[] { "en_GB" });

        command.doPost(request, response);

        JsonNode response = ParsingUtilities.mapper.readValue(writer.toString(), JsonNode.class);
        assertTrue(response.has("dictionary"));
        assertEquals(response.get("lang").asText(), "en_GB");
    }

    public void testLoadMultiLanguages() throws ServletException, IOException {
        when(request.getParameter("module")).thenReturn("core");
        when(request.getParameterValues("lang")).thenReturn(new String[] { "ja", "it", "es", "de" });

        command.doPost(request, response);

        JsonNode response = ParsingUtilities.mapper.readValue(writer.toString(), JsonNode.class);
        assertTrue(response.has("dictionary"));
        assertEquals(response.get("lang").asText(), "ja");
    }

    @Test
    public void testLoadUnknownLanguage() throws ServletException, IOException {
        when(request.getParameter("module")).thenReturn("core");
        when(request.getParameterValues("lang")).thenReturn(new String[] { "foobar" });

        command.doPost(request, response);

        JsonNode response = ParsingUtilities.mapper.readValue(writer.toString(), JsonNode.class);
        assertTrue(response.has("dictionary"));
        assertEquals(response.get("lang").asText(), "en");
    }

    @Test
    public void testLoadNoLanguage() throws JsonParseException, JsonMappingException, IOException, ServletException {
        when(request.getParameter("module")).thenReturn("core");
        // when(request.getParameter("lang")).thenReturn("");
        when(request.getParameterValues("lang")).thenReturn(new String[] { "" });

        command.doPost(request, response);

        JsonNode response = ParsingUtilities.mapper.readValue(writer.toString(), JsonNode.class);
        assertTrue(response.has("dictionary"));
        assertEquals(response.get("lang").asText(), "en");
    }

    @Test
    public void testLoadNullLanguage() throws JsonParseException, JsonMappingException, IOException, ServletException {
        when(request.getParameter("module")).thenReturn("core");
        when(request.getParameterValues("lang")).thenReturn(null);

        command.doPost(request, response);

        JsonNode response = ParsingUtilities.mapper.readValue(writer.toString(), JsonNode.class);
        assertTrue(response.has("dictionary"));
        assertEquals(response.get("lang").asText(), "en");
    }

    @Test
    public void testLoadLanguageWithDirectorySlip() throws JsonParseException, JsonMappingException, IOException, ServletException {
        when(request.getParameter("module")).thenReturn("core");
        when(request.getParameterValues("lang")).thenReturn(new String[] { "../../../secrets" });

        command.doPost(request, response);

        JsonNode response = ParsingUtilities.mapper.readValue(writer.toString(), JsonNode.class);
        assertTrue(response.has("dictionary"));
        assertEquals(response.get("lang").asText(), "en");
    }

    @Test
    public void testLanguageFallback() throws JsonParseException, JsonMappingException, IOException {
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

    // tests that valid language files return the correct JSON when called by loadLanguage
    @Test
    public void testLoadLanguageWithValidFile() throws IOException {
        File moduleDir = createLangsDir();
        Files.writeString(new File(moduleDir, "langs/translation-valid.json").toPath(),
                "{\"greeting\": \"hello\"}");
        RefineServlet servlet = mockServletFor(moduleDir);

        ObjectNode result = LoadLanguageCommand.loadLanguage(servlet, "test", "valid");

        assertNotNull(result);
        assertEquals(result.get("greeting").asText(), "hello");
    }

    // tests that missing language files return null when called by loadLanguage
    @Test
    public void testLoadLanguageWithMissingFile() throws IOException {
        File moduleDir = createLangsDir();
        RefineServlet servlet = mockServletFor(moduleDir);

        ObjectNode result = LoadLanguageCommand.loadLanguage(servlet, "test", "zz");

        assertNull(result);
    }

    // tests that malformed language files return null when called by loadLanguage
    @Test
    public void testLoadLanguageWithMalformedFile() throws IOException {
        File moduleDir = createLangsDir();
        Files.writeString(new File(moduleDir, "langs/translation-broken.json").toPath(),
                "{\"greeting\":");
        RefineServlet servlet = mockServletFor(moduleDir);

        ObjectNode result = LoadLanguageCommand.loadLanguage(servlet, "test", "broken");

        assertNull(result);
    }

    // creates a temporary directory with a subdirectory called langs
    private File createLangsDir() throws IOException {
        File moduleDir = TestUtils.createTempDirectory("openrefine-test-lang-module");
        File langsDir = new File(moduleDir, "langs");
        langsDir.mkdirs();
        return moduleDir;
    }

    // creates a fake servlet that tells load language to load from the temp folder
    private RefineServlet mockServletFor(File moduleDir) {
        ButterflyModule module = mock(ButterflyModule.class);
        when(module.getPath()).thenReturn(moduleDir);
        RefineServlet servlet = mock(RefineServlet.class);
        when(servlet.getModule("test")).thenReturn(module);
        return servlet;
    }
}
