package org.openrefine.wikidata.commands;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.openrefine.wikidata.testing.TestingData.jsonFromFile;

import java.io.IOException;

import javax.servlet.ServletException;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class SaveWikibaseSchemaCommandTest extends SchemaCommandTest {
    
    @BeforeMethod
    public void setUp() {
        this.command = new SaveWikibaseSchemaCommand();
    }
    
    @Test
    public void testValidSchema() throws ServletException, IOException {
        String schemaJson = jsonFromFile("data/schema/inception.json").toString();
        when(request.getParameter("schema")).thenReturn(schemaJson);
        
        command.doPost(request, response);
        
        assertTrue(writer.toString().contains("\"ok\""));
    }
}
