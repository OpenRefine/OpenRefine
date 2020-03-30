package com.google.refine.commands;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.util.ParsingUtilities;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;

public class GetCSRFTokenCommandTest {
    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
	protected StringWriter writer = null;
	protected Command command = null;
	
    @BeforeMethod
    public void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        command = new GetCSRFTokenCommand();
        writer = new StringWriter();
        try {
            when(response.getWriter()).thenReturn(new PrintWriter(writer));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void testGetToken() throws JsonParseException, JsonMappingException, IOException, ServletException {
    	command.doGet(request, response);
    	ObjectNode result = ParsingUtilities.mapper.readValue(writer.toString(), ObjectNode.class);
    	String token = result.get("token").asText();
    	assertTrue(Command.csrfFactory.validToken(token));
    }
}
