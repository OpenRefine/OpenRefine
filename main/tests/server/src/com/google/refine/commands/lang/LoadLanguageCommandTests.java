package com.google.refine.commands.lang;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.RefineServlet;
import com.google.refine.commands.CommandTestBase;
import com.google.refine.util.ParsingUtilities;

import edu.mit.simile.butterfly.ButterflyModule;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
}

