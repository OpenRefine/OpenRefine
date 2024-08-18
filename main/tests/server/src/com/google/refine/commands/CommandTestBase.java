
package com.google.refine.commands;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.BeforeMethod;

import com.google.refine.RefineTest;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class CommandTestBase extends RefineTest {

    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
    protected Command command = null;
    protected StringWriter writer = null;

    @BeforeMethod
    public void setUpRequestResponse() throws IOException {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));
    }

    /**
     * Convenience method to check that CSRF protection was triggered
     */
    protected void assertCSRFCheckFailed() {
        TestUtils.assertEqualsAsJson(writer.toString(), "{\"code\":\"error\",\"message\":\"Missing or invalid csrf_token parameter\"}");
    }

    /**
     * Convenience method to check that CSRF protection was NOT triggered
     */
    protected void assertErrorNotCSRF() throws JsonProcessingException {
        String response = writer.toString();
        JsonNode node = ParsingUtilities.mapper.readValue(response, JsonNode.class);
        assertEquals(node.get("code").toString(), "\"error\"");
        assertFalse(response.contains("Missing or invalid csrf_token parameter"));
    }
}
