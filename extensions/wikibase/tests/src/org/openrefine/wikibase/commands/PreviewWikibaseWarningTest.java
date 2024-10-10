/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2024 Sunil Natraj
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

import java.io.Serializable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

import org.openrefine.wikibase.testing.TestingData;

public class PreviewWikibaseWarningTest extends SchemaCommandTest {

    Project project;

    @BeforeMethod
    public void SetUp() {
        command = new PreviewWikibaseSchemaCommand();

        project = createProject("wiki-warnings-test",
                new String[] { "Column 1", "male population", "population", "country" },
                new Serializable[][] {
                        { "Wikidata Sandbox 3", "111", "121", "India" }
                });
        project.rows.get(0).cells.set(0, TestingData.makeMatchedCell("Q15397819", "Wikidata Sandbox 3"));
        project.rows.get(0).cells.set(3, TestingData.makeMatchedCell("Q668", "India"));
    }

    @AfterMethod
    public void tearDown() {
    }

    @Test
    public void testWarningData() throws Exception {

        String schemaJson = jsonFromFile("schema/warning_data_test.json");
        String manifestJson = jsonFromFile("manifest/wikidata-manifest-v1.0.json");
        when(request.getParameter("project")).thenReturn(String.valueOf(project.id));
        when(request.getParameter("schema")).thenReturn(schemaJson);
        when(request.getParameter("manifest")).thenReturn(manifestJson);

        command.doPost(request, response);

        ObjectNode response = ParsingUtilities.evaluateJsonStringToObjectNode(writer.toString());
        ArrayNode issues = (ArrayNode) response.get("warnings");
        for (JsonNode node : issues) {
            // Read aggregationId
            String aggregationId = node.get("aggregationId").asText();
            // Check for nested property properties/missing_property_entity/label
            JsonNode missingPropertyLabel = node.path("properties").path("missing_property_entity").path("label");
            JsonNode addedPropertyLabel = node.path("properties").path("added_property_entity").path("label");

            if (aggregationId.equals("missing-mandatory-qualifiers_P1082-P459")) {
                assertEquals(missingPropertyLabel.asText(), "determination method or standard");
            } else if (aggregationId.equals("missing-mandatory-qualifiers_P1540-P585")) {
                assertEquals(missingPropertyLabel.asText(), "point in time");
            } else if (aggregationId.equals("existing-item-requires-certain-other-statement_P1540P1539")) {
                assertEquals(addedPropertyLabel.asText(), "female population");
            }
        }
    }
}
