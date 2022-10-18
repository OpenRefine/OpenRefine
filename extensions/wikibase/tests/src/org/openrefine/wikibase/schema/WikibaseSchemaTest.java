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

package org.openrefine.wikibase.schema;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openrefine.wikibase.schema.strategies.StatementEditingMode;
import org.openrefine.wikibase.schema.strategies.StatementMerger;
import org.openrefine.wikibase.schema.validation.PathElement;
import org.openrefine.wikibase.schema.validation.PathElement.Type;
import org.openrefine.wikibase.schema.validation.ValidationError;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.openrefine.wikibase.testing.TestingData;
import org.openrefine.wikibase.testing.WikidataRefineTest;
import org.openrefine.wikibase.updates.EntityEdit;
import org.openrefine.wikibase.updates.ItemEditBuilder;
import org.openrefine.wikibase.updates.StatementEdit;
import org.openrefine.wikibase.updates.TermedStatementEntityEdit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.*;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.Project;
import com.google.refine.util.TestUtils;

public class WikibaseSchemaTest extends WikidataRefineTest {

    private ItemIdValue qid1 = Datamodel.makeWikidataItemIdValue("Q1377");
    private ItemIdValue qid2 = Datamodel.makeWikidataItemIdValue("Q865528");
    private TimeValue date1 = Datamodel.makeTimeValue(1919, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 9,
            (byte) 0, (byte) 0, (byte) 0, TimeValue.CM_GREGORIAN_PRO);
    private TimeValue date2 = Datamodel.makeTimeValue(1965, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 9,
            (byte) 0, (byte) 0, (byte) 0, TimeValue.CM_GREGORIAN_PRO);
    private StringValue url = Datamodel.makeStringValue("http://www.ljubljana-slovenia.com/university-ljubljana");
    private PropertyIdValue inceptionPid = Datamodel.makeWikidataPropertyIdValue("P571");
    private PropertyIdValue refPid = Datamodel.makeWikidataPropertyIdValue("P854");
    private PropertyIdValue retrievedPid = Datamodel.makeWikidataPropertyIdValue("P813");
    private Snak refSnak = Datamodel.makeValueSnak(refPid, url);
    private Snak retrievedSnak = Datamodel.makeValueSnak(retrievedPid,
            Datamodel.makeTimeValue(2018, (byte) 2, (byte) 28, (byte) 0, (byte) 0, (byte) 0, (byte) 11,
                    (byte) 0, (byte) 0, (byte) 0, TimeValue.CM_GREGORIAN_PRO));
    private Snak mainSnak1 = Datamodel.makeValueSnak(inceptionPid, date1);
    private Snak mainSnak2 = Datamodel.makeValueSnak(inceptionPid, date2);
    private Claim claim1 = Datamodel.makeClaim(qid1, mainSnak1, Collections.emptyList());
    private Claim claim2 = Datamodel.makeClaim(qid2, mainSnak2, Collections.emptyList());
    private SnakGroup refSnakGroup = Datamodel.makeSnakGroup(Collections.singletonList(refSnak));
    private SnakGroup retrievedSnakGroup = Datamodel.makeSnakGroup(Collections.singletonList(retrievedSnak));
    private Statement statement1 = Datamodel.makeStatement(claim1,
            Collections.singletonList(Datamodel.makeReference(Arrays.asList(refSnakGroup, retrievedSnakGroup))),
            StatementRank.NORMAL, "");
    private StatementEdit statementUpdate1 = new StatementEdit(
            statement1,
            StatementMerger.FORMER_DEFAULT_STRATEGY,
            StatementEditingMode.ADD_OR_MERGE);
    private Statement statement2 = Datamodel.makeStatement(claim2,
            Collections.singletonList(Datamodel.makeReference(Collections.singletonList(retrievedSnakGroup))),
            StatementRank.NORMAL, "");
    private StatementEdit statementUpdate2 = new StatementEdit(
            statement2,
            StatementMerger.FORMER_DEFAULT_STRATEGY,
            StatementEditingMode.ADD_OR_MERGE);

    private Project project;

    @BeforeMethod
    public void setUpProject() {
        project = this.createCSVProject(TestingData.inceptionCsv);
        project.rows.get(0).cells.set(0, TestingData.makeMatchedCell("Q1377", "University of Ljubljana"));
        project.rows.get(1).cells.set(0, TestingData.makeMatchedCell("Q865528", "University of Warwick"));
    }

    @Test
    public void testSerialize()
            throws IOException {
        String serialized = TestingData.jsonFromFile("schema/history_of_medicine.json");
        WikibaseSchema parsed = WikibaseSchema.reconstruct(serialized);
        TestUtils.isSerializedTo(parsed, TestingData.jsonFromFile("schema/history_of_medicine_normalized.json").toString());
    }

    @Test
    public void testDeserializeMediaInfo() throws IOException {
        String serialized = TestingData.jsonFromFile("schema/with_mediainfo.json");
        WikibaseSchema parsed = WikibaseSchema.reconstruct(serialized);
        TestUtils.isSerializedTo(parsed, TestingData.jsonFromFile("schema/with_mediainfo.json"));
    }

    @Test
    public void testDeserialize()
            throws IOException {
        // this json file was generated by an earlier version of the software
        // it contains extra "type" fields that are now ignored.
        String serialized = TestingData.jsonFromFile("schema/roarmap.json");
        WikibaseSchema schema = WikibaseSchema.reconstruct(serialized);
        // Check that we fall back on Wikidata if no API endpoint was supplied
        assertEquals(schema.getMediaWikiApiEndpoint(), ApiConnection.URL_WIKIDATA_API);
    }

    @Test
    public void testEvaluate()
            throws IOException {
        String serialized = TestingData.jsonFromFile("schema/inception.json");
        WikibaseSchema schema = WikibaseSchema.reconstruct(serialized);

        // Validate the schema
        ValidationState validation = new ValidationState(project.columnModel);
        schema.validate(validation);
        assertTrue(validation.getValidationErrors().isEmpty());

        Engine engine = new Engine(project);
        List<EntityEdit> updates = schema.evaluate(project, engine);
        List<EntityEdit> expected = new ArrayList<>();
        TermedStatementEntityEdit update1 = new ItemEditBuilder(qid1).addStatement(statementUpdate1).build();
        expected.add(update1);
        TermedStatementEntityEdit update2 = new ItemEditBuilder(qid2).addStatement(statementUpdate2).build();
        expected.add(update2);
        assertEquals(expected, updates);
    }

    @Test
    public void testValidate() throws IOException {
        String serialized = TestingData.jsonFromFile("schema/inception_with_errors.json");
        WikibaseSchema schema = WikibaseSchema.reconstruct(serialized);

        // Validate the schema
        ValidationState validation = new ValidationState(project.columnModel);
        schema.validate(validation);

        List<ValidationError> expectedErrors = new ArrayList<>();
        expectedErrors.add(new ValidationError(Arrays.asList(
                new PathElement(Type.ENTITY, 0),
                new PathElement(Type.STATEMENT, "inception (P571)"),
                new PathElement(Type.REFERENCE, 0),
                new PathElement(Type.VALUE, "reference URL (P854)")),
                "Column 'nonexisting_column_name' does not exist"));
        expectedErrors.add(new ValidationError(Arrays.asList(
                new PathElement(Type.ENTITY, 0),
                new PathElement(Type.STATEMENT, "inception (P571)"),
                new PathElement(Type.REFERENCE, 0),
                new PathElement(Type.VALUE, "retrieved (P813)")),
                "Empty date field"));

        assertEquals(validation.getValidationErrors(), expectedErrors);
    }

    @Test
    public void testDeserializeEmpty() throws IOException {
        String schemaJson = "{\"itemDocuments\":[{\"statementGroups\":[{\"statements\":[]}],"
                + "\"nameDescs\":[]}],\"siteIri\":\"http://www.wikidata.org/entity/\"}";
        WikibaseSchema schema = WikibaseSchema.reconstruct(schemaJson);
        ValidationState validationContext = new ValidationState(new ColumnModel());
        schema.validate(validationContext);
        assertFalse(validationContext.getValidationErrors().isEmpty());
    }

    @Test
    public void testEvaluateRespectsFacets()
            throws IOException {
        String serialized = TestingData.jsonFromFile("schema/inception.json");
        WikibaseSchema schema = WikibaseSchema.reconstruct(serialized);

        // Validate the schema
        ValidationState validation = new ValidationState(project.columnModel);
        schema.validate(validation);
        assertTrue(validation.getValidationErrors().isEmpty());

        Engine engine = new Engine(project);
        EngineConfig engineConfig = EngineConfig.reconstruct("{\n"
                + "      \"mode\": \"row-based\",\n"
                + "      \"facets\": [\n"
                + "        {\n"
                + "          \"mode\": \"text\",\n"
                + "          \"invert\": false,\n"
                + "          \"caseSensitive\": false,\n"
                + "          \"query\": \"www\",\n"
                + "          \"name\": \"reference\",\n"
                + "          \"type\": \"text\",\n"
                + "          \"columnName\": \"reference\"\n"
                + "        }\n"
                + "      ]\n"
                + "    }");
        engine.initializeFromConfig(engineConfig);
        List<EntityEdit> updates = schema.evaluate(project, engine);
        List<EntityEdit> expected = new ArrayList<>();
        EntityEdit update1 = new ItemEditBuilder(qid1).addStatement(statementUpdate1).build();
        expected.add(update1);
        assertEquals(expected, updates);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testUnmodifiableList() throws IOException {
        String serialized = TestingData.jsonFromFile("schema/inception.json");
        WikibaseSchema schema = WikibaseSchema.reconstruct(serialized);
        schema.getEntityDocumentExpressions().clear();
    }
}
