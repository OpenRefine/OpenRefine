package com.google.refine.tests.commands.browsing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.commands.Command;
import com.google.refine.commands.browsing.GetClusteringFunctionsAndDistancesCommand;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;


public class GetClusteringFunctionsAndDistancesCommandTest {
	
    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
	protected StringWriter writer = null;
	protected Command command = null;
	
    @BeforeMethod
    public void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        command = new GetClusteringFunctionsAndDistancesCommand();
        writer = new StringWriter();
        try {
            when(response.getWriter()).thenReturn(new PrintWriter(writer));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
    @Test
	public void testGetFunctionsAndKeyers() throws ServletException, IOException {
    	command.doGet(request, response);
    	ObjectNode result = ParsingUtilities.mapper.readValue(writer.toString(), ObjectNode.class);
    	assertTrue(Arrays.asList(JSONUtilities.getStringArray(result, "keyers")).contains("metaphone"));
    	assertTrue(Arrays.asList(JSONUtilities.getStringArray(result, "distances")).contains("levenshtein"));
	}
}
