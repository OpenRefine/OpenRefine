
package org.openrefine.wikibase.commands;

import static org.mockito.Mockito.when;
import static org.openrefine.wikibase.testing.TestingData.jsonFromFile;
import static org.testng.Assert.assertEquals;

import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;

public class ParseWikibaseSchemaCommandTest extends CommandTest {

    @BeforeMethod
    public void setUp() {
        this.command = new ParseWikibaseSchemaCommand();
    }

    @Test
    public void testNoSchemaTemplate()
            throws Exception {
        command.doPost(request, response);

        assertEquals(writer.toString(), "{\"code\":\"error\",\"message\":\"No Wikibase schema template provided.\"}");
    }

    @Test(expectedExceptions = JsonParseException.class)
    public void testInvalidJson()
            throws Exception {
        when(request.getParameter("template")).thenReturn("{bogus json");
        command.doPost(request, response);
    }

    @Test(expectedExceptions = JacksonException.class)
    public void testInvalidSchemaTemplate()
            throws Exception {
        when(request.getParameter("template")).thenReturn("{\"entityEdits\":\"foo\"}");
        command.doPost(request, response);
    }

    @Test
    public void testIncompleteSchema() throws Exception {
        // schema that is syntactically correct but misses some elements.
        // it is invalid as a schema but valid as a schema template.
        String schemaJson = jsonFromFile("schema/inception_with_errors.json").toString();
        String templateJson = "{\"name\":\"My template\",\"schema\":" + schemaJson + "}";
        when(request.getParameter("template")).thenReturn(templateJson);

        command.doPost(request, response);

        String expectedMessage = "{"
                + "\"code\":\"ok\","
                + "\"object_type\":\"template\","
                + "\"message\":\"Valid schema template\"}";

        TestUtils.assertEqualsAsJson(writer.toString(), expectedMessage);
    }

    @Test
    public void testIncompleteSchemaWithoutName() throws Exception {
        // schema that is syntactically correct but misses some elements.
        // it is invalid as a schema but valid as a schema template.
        String schemaJson = jsonFromFile("schema/inception_with_errors.json").toString();
        when(request.getParameter("template")).thenReturn(schemaJson);

        command.doPost(request, response);

        String expectedMessage = "{"
                + "\"code\":\"ok\","
                + "\"object_type\":\"schema\","
                + "\"message\":\"Valid schema\"}";

        TestUtils.assertEqualsAsJson(writer.toString(), expectedMessage);
    }
}
