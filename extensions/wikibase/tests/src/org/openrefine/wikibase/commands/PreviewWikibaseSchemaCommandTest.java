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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.openrefine.wikibase.testing.TestingData.jsonFromFile;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Locale;

import javax.servlet.ServletException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;

import com.google.refine.util.LocaleUtils;
import com.google.refine.util.ParsingUtilities;

import org.openrefine.wikibase.testing.TestingData;
import org.openrefine.wikibase.utils.EntityCache;
import org.openrefine.wikibase.utils.EntityCacheStub;

public class PreviewWikibaseSchemaCommandTest extends SchemaCommandTest {

    private String localeSetting;

    @BeforeMethod
    public void SetUp() {
        command = new PreviewWikibaseSchemaCommand();
        EntityCacheStub entityCacheStub = new EntityCacheStub();
        EntityCache.setEntityCache("http://www.wikidata.org/entity/", entityCacheStub);

        project = createProject("wiki-warnings-test",
                new String[] { "Column 1", "Quebec cultural heritage directory ID" },
                new Serializable[][] {
                        { "Habitat 67", "98890" }
                });
        project.rows.get(0).cells.set(0, TestingData.makeMatchedCell("Q1032248", "Habitat 67"));
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

    @BeforeMethod
    public void setLocale() {
        localeSetting = Locale.getDefault().getLanguage();
        LocaleUtils.setLocale("en");
    }

    @AfterMethod
    public void unsetLocale() {
        LocaleUtils.setLocale(localeSetting);
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
        boolean existingitemrequirescertainotherstatementwithsuggestedvalue_P633P17 = false;
        boolean existingitemrequirescertainotherstatement_P633P18 = false;
        for (JsonNode node : issues) {
            // Read aggregationId
            String aggregationId = node.get("aggregationId").asText();
            // Check for nested property properties/missing_property_entity/label
            JsonNode addedPropertyLabel = node.path("properties").path("added_property_entity").path("label");
            JsonNode itemEntityLabel = node.path("properties").path("item_entity").path("label");

            if (aggregationId.equals("existing-item-requires-property-to-have-certain-values-with-suggested-value_P633P17")) {
                assertEquals(addedPropertyLabel.asText(), "country");
                assertEquals(itemEntityLabel.asText(), "Canada");
                existingitemrequirescertainotherstatementwithsuggestedvalue_P633P17 = true;
            } else if (aggregationId.equals("existing-item-requires-certain-other-statement_P633P18")) {
                assertEquals(addedPropertyLabel.asText(), "image");
                existingitemrequirescertainotherstatement_P633P18 = true;
            }
        }
        assertFalse(existingitemrequirescertainotherstatementwithsuggestedvalue_P633P17);
        assertFalse(existingitemrequirescertainotherstatement_P633P18);
    }

    @DataProvider(name = "inspectionModes")
    public Object[][] inspectionModes() {
        return new Object[][] { { false }, { true } };
    }

    @Test(dataProvider = "inspectionModes")
    public void testEntityKindMismatch(boolean slowMode) throws Exception {
        ObjectNode preview = previewItemValue("P8017", "wikibase-form", slowMode);
        JsonNode mismatch = null;
        for (JsonNode warning : preview.path("warnings")) {
            if ("property-datatype-mismatch".equals(warning.path("type").asText())) {
                mismatch = warning;
            }
        }
        assertNotNull(mismatch, "Preview should report an Item value supplied to a Form-valued property");
        assertEquals(mismatch.path("properties").path("property_entity").path("id").asText(), "P8017");
        assertEquals(mismatch.path("properties").path("expected_entity_type").asText(), "Form");
        assertEquals(mismatch.path("properties").path("actual_entity_type").asText(), "Item");
        assertEquals(mismatch.path("count").asInt(), 1);
    }

    @Test(dataProvider = "inspectionModes")
    public void testMatchingEntityKind(boolean slowMode) throws Exception {
        ObjectNode preview = previewItemValue("P31", "wikibase-item", slowMode);
        for (JsonNode warning : preview.path("warnings")) {
            assertFalse("property-datatype-mismatch".equals(warning.path("type").asText()));
        }
    }

    private ObjectNode previewItemValue(String propertyId, String datatype, boolean slowMode) throws Exception {
        project = createProject("entity-kind-preview", new String[] { "Subject", "Value" },
                new Serializable[][] { { "Synthetic subject", "Synthetic Item value" } });
        project.rows.get(0).cells.set(0, TestingData.makeMatchedCell("Q1032248", "Synthetic subject"));
        project.rows.get(0).cells.set(1, TestingData.makeMatchedCell("Q5", "Synthetic Item value"));

        PropertyDocument property = mock(PropertyDocument.class);
        when(property.getDatatype()).thenReturn(Datamodel.makeDatatypeIdValueFromJsonString(datatype));
        EntityCache cache = mock(EntityCache.class);
        when(cache.get(any(EntityIdValue.class))).thenReturn(property);
        when(cache.getMultipleDocuments(anyList())).thenReturn(Collections.singletonList(property));
        EntityCache.setEntityCache(Datamodel.SITE_WIKIDATA, cache);

        ObjectNode schema = ParsingUtilities.evaluateJsonStringToObjectNode(jsonFromFile("schema/entity_kind_preview.json"));
        ObjectNode schemaProperty = (ObjectNode) schema.path("entityEdits").get(0).path("statementGroups").get(0).path("property");
        schemaProperty.put("pid", propertyId);
        schemaProperty.put("datatype", datatype);
        when(request.getParameter("project")).thenReturn(String.valueOf(project.id));
        when(request.getParameter("schema")).thenReturn(schema.toString());
        when(request.getParameter("manifest")).thenReturn(jsonFromFile("manifest/wikidata-manifest-v1.0.json"));
        when(request.getParameter("slow_mode")).thenReturn(Boolean.toString(slowMode));

        command.doPost(request, response);

        ObjectNode preview = ParsingUtilities.evaluateJsonStringToObjectNode(writer.toString());
        assertEquals(preview.path("edits_preview").size(), 1, preview.toString());
        return preview;
    }
}
