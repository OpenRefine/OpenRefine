package org.openrefine.wikidata.commands;

import javax.servlet.ServletException;

import org.json.JSONException;
import org.json.JSONObject;
import org.openrefine.wikidata.testing.TestingData;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;

import com.google.refine.util.ParsingUtilities;

import static org.openrefine.wikidata.testing.TestingData.jsonFromFile;

public class PreviewWikibaseSchemaCommandTest extends SchemaCommandTest {
    
    @BeforeMethod
    public void SetUp() throws JSONException {
        command = new PreviewWikibaseSchemaCommand();
    }
    
    @Test
    public void testValidSchema() throws JSONException, IOException, ServletException {
        String schemaJson = jsonFromFile("data/schema/inception.json").toString();
        when(request.getParameter("schema")).thenReturn(schemaJson);
        
        command.doPost(request, response);
        
        JSONObject response = ParsingUtilities.evaluateJsonStringToObject(writer.toString());
        assertEquals(TestingData.inceptionWithNewQS, response.getString("quickstatements"));
    }

}
