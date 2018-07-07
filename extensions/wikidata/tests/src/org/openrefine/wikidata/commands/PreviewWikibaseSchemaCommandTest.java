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
package org.openrefine.wikidata.commands;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.openrefine.wikidata.testing.TestingData.jsonFromFile;

import java.io.IOException;

import javax.servlet.ServletException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.openrefine.wikidata.testing.TestingData;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.util.ParsingUtilities;

public class PreviewWikibaseSchemaCommandTest extends SchemaCommandTest {

    @BeforeMethod
    public void SetUp()
            throws JSONException {
        command = new PreviewWikibaseSchemaCommand();
    }

    @Test
    public void testValidSchema()
            throws JSONException, IOException, ServletException {
        String schemaJson = jsonFromFile("data/schema/inception.json").toString();
        when(request.getParameter("schema")).thenReturn(schemaJson);

        command.doPost(request, response);

        JSONObject response = ParsingUtilities.evaluateJsonStringToObject(writer.toString());
        JSONArray edits = response.getJSONArray("edits_preview");
        assertEquals(3, edits.length());
    }

}
