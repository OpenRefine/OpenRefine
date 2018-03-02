package org.openrefine.wikidata.commands;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.ServletException;

public abstract class SchemaCommandTest extends CommandTest {
    
    @Test
    public void testNoSchema() throws ServletException, IOException {
        command.doPost(request, response);
        
        assertEquals("{\"status\":\"error\",\"message\":\"No Wikibase schema provided.\"}", writer.toString());
    }
    
    @Test
    public void testInvalidSchema() throws ServletException, IOException {
        when(request.getParameter("schema")).thenReturn("{bogus json");
        command.doPost(request, response);
        
        assertEquals("{\"status\":\"error\",\"message\":\"Wikibase schema could not be parsed.\"}", writer.toString());
    }
}
