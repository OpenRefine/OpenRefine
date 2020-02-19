package com.google.refine.commands.lang;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.RefineServlet;
import com.google.refine.commands.CommandTestBase;
import com.google.refine.util.ParsingUtilities;

import edu.mit.simile.butterfly.ButterflyModule;

public class LoadLanguageCommandTests extends CommandTestBase {
	
	@BeforeMethod
	public void setUpCommand() {
		command = new LoadLanguageCommand();
		ButterflyModule coreModule = mock(ButterflyModule.class);
		
		when(coreModule.getName()).thenReturn("core");
		when(coreModule.getPath()).thenReturn(new File("webapp/modules/core"));
		RefineServlet servlet = mock(RefineServlet.class);
		when(servlet.getModule("core")).thenReturn(coreModule);
		command.init(servlet);
	}
	
	@Test
	public void testLoadLanguages() throws ServletException, IOException {
		when(request.getParameter("module")).thenReturn("core");
		when(request.getParameterValues("lang")).thenReturn(new String[] {"en"});
		
		command.doPost(request, response);
		
		JsonNode response = ParsingUtilities.mapper.readValue(writer.toString(), JsonNode.class);
		assertTrue(response.has("dictionary"));
		assertTrue(response.has("lang"));
	}
	
	@Test
	public void testLoadUnknownLanguage() throws ServletException, IOException {
		when(request.getParameter("module")).thenReturn("core");
		when(request.getParameterValues("lang")).thenReturn(new String[] {"foobar"});
		
		command.doPost(request, response);
		
		JsonNode response = ParsingUtilities.mapper.readValue(writer.toString(), JsonNode.class);
		assertTrue(response.has("dictionary"));
		assertEquals(response.get("lang").asText(), "en");
	}
	
	@Test
	public void testLoadNoLanguage() throws JsonParseException, JsonMappingException, IOException, ServletException {
	    when(request.getParameter("module")).thenReturn("core");
	    when(request.getParameter("lang")).thenReturn("");
	    
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
}

