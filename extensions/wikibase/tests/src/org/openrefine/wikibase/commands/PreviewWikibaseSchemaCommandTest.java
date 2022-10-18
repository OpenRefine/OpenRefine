/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package org.openrefine.wikibase.commands;

import static org.mockito.Mockito.when;
import static org.openrefine.wikibase.testing.TestingData.jsonFromFile;
import static org.testng.Assert.assertEquals;

import java.io.IOException;

import javax.servlet.ServletException;

import org.openrefine.wikibase.utils.EntityCache;
import org.openrefine.wikibase.utils.EntityCacheStub;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.util.ParsingUtilities;

public class PreviewWikibaseSchemaCommandTest extends SchemaCommandTest {

    @BeforeMethod
    public void SetUp() {
        command = new PreviewWikibaseSchemaCommand();
        EntityCacheStub entityCacheStub = new EntityCacheStub();
        EntityCache.setEntityCache("http://www.wikidata.org/entity/", entityCacheStub);
    }

    @AfterMethod
    public void tearDown() {
        EntityCache.removeEntityCache("http://www.wikidata.org/entity/");
    }

    @Test
    public void testValidSchema() throws Exception {

        String schemaJson = jsonFromFile("schema/inception.json");
        String manifestJson = jsonFromFile("manifest/wikidata-manifest-v1.0.json");
        when(request.getParameter("schema")).thenReturn(schemaJson);
        when(request.getParameter("manifest")).thenReturn(manifestJson);

        command.doPost(request, response);

        ObjectNode response = ParsingUtilities.evaluateJsonStringToObjectNode(writer.toString());
        ArrayNode edits = (ArrayNode) response.get("edits_preview");
        assertEquals(edits.size(), 3);
        ArrayNode issues = (ArrayNode) response.get("warnings");
        assertEquals(issues.size(), 4);
    }

    @Test
    public void testIncompleteSchema() throws IOException, ServletException {
        String schemaJson = jsonFromFile("schema/inception_with_errors.json");
        String manifestJson = jsonFromFile("manifest/wikidata-manifest-v1.0.json");
        when(request.getParameter("schema")).thenReturn(schemaJson);
        when(request.getParameter("manifest")).thenReturn(manifestJson);

        command.doPost(request, response);

        ObjectNode response = ParsingUtilities.evaluateJsonStringToObjectNode(writer.toString());
        ArrayNode validationErrors = (ArrayNode) response.get("errors");
        assertEquals(validationErrors.size(), 2);
    }

    @Test
    public void testNoManifest() throws IOException, ServletException {
        String schemaJson = jsonFromFile("schema/inception.json");
        when(request.getParameter("schema")).thenReturn(schemaJson);

        command.doPost(request, response);

        assertEquals(writer.toString(), "{\"code\":\"error\",\"message\":\"No Wikibase manifest provided.\"}");
    }

    @Test
    public void testInvalidManifest() throws IOException, ServletException {
        String schemaJson = jsonFromFile("schema/inception.json");
        String manifestJson = "{ invalid manifest";
        when(request.getParameter("schema")).thenReturn(schemaJson);
        when(request.getParameter("manifest")).thenReturn(manifestJson);

        command.doPost(request, response);

        assertEquals(writer.toString(),
                "{\"code\":\"error\",\"message\":\"Wikibase manifest could not be parsed. Error message: invalid manifest format\"}");
    }
}
