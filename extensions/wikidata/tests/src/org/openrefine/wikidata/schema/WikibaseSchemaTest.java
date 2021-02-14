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

package org.openrefine.wikidata.schema;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

import org.openrefine.RefineTest;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.FacetConfigResolver;
import org.openrefine.browsing.facets.TextSearchFacet.TextSearchFacetConfig;
import org.openrefine.model.GridState;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;

public class WikibaseSchemaTest extends RefineTest {

    private ItemIdValue qid1 = Datamodel.makeWikidataItemIdValue("Q1377");
    private ItemIdValue qid2 = Datamodel.makeWikidataItemIdValue("Q865528");
    private TimeValue date1 = Datamodel.makeTimeValue(1919, (byte) 1, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 9,
            (byte) 0, (byte) 0, (byte) 0, TimeValue.CM_GREGORIAN_PRO);
    private TimeValue date2 = Datamodel.makeTimeValue(1965, (byte) 1, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 9,
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
    private Statement statement2 = Datamodel.makeStatement(claim2,
            Collections.singletonList(Datamodel.makeReference(Collections.singletonList(retrievedSnakGroup))),
            StatementRank.NORMAL, "");

    private GridState grid;

    @BeforeMethod
    public void setUpProject() {
        grid = this.createGrid(
                new String[] { "subject", "inception", "reference" },
                new Serializable[][] {
                        { TestingData.makeMatchedCell("Q1377", "University of Ljubljana"), "1919",
                                "http://www.ljubljana-slovenia.com/university-ljubljana" },
                        { TestingData.makeMatchedCell("Q865528", "University of Warwick"), "1965", "" } });
        FacetConfigResolver.registerFacetConfig("core", "text", TextSearchFacetConfig.class);
    }

    @Test
    public void testSerialize()
            throws IOException {
        String serialized = TestingData.jsonFromFile("schema/history_of_medicine.json");
        WikibaseSchema parsed = WikibaseSchema.reconstruct(serialized);
        TestUtils.isSerializedTo(parsed, TestingData.jsonFromFile("schema/history_of_medicine_normalized.json").toString(),
                ParsingUtilities.defaultWriter);
    }

    @Test
    public void testDeserialize()
            throws IOException {
        // this json file was generated by an earlier version of the software
        // it contains extra "type" fields that are now ignored.
        String serialized = TestingData.jsonFromFile("schema/roarmap.json");
        WikibaseSchema.reconstruct(serialized);
    }

    @Test
    public void testEvaluate()
            throws IOException {
        String serialized = TestingData.jsonFromFile("schema/inception.json");
        WikibaseSchema schema = WikibaseSchema.reconstruct(serialized);
        Engine engine = new Engine(grid, EngineConfig.ALL_ROWS);
        List<ItemUpdate> updates = schema.evaluate(grid, engine);
        List<ItemUpdate> expected = new ArrayList<>();
        ItemUpdate update1 = new ItemUpdateBuilder(qid1).addStatement(statement1).build();
        expected.add(update1);
        ItemUpdate update2 = new ItemUpdateBuilder(qid2).addStatement(statement2).build();
        expected.add(update2);
        assertEquals(expected, updates);
    }

    @Test(expectedExceptions = IOException.class)
    public void testDeserializeEmpty() throws IOException {
        String schemaJson = "{\"itemDocuments\":[{\"statementGroups\":[{\"statements\":[]}],"
                + "\"nameDescs\":[]}],\"wikibasePrefix\":\"http://www.wikidata.org/entity/\"}";
        WikibaseSchema.reconstruct(schemaJson);
    }

    @Test
    public void testEvaluateRespectsFacets()
            throws IOException {
        String serialized = TestingData.jsonFromFile("schema/inception.json");
        WikibaseSchema schema = WikibaseSchema.reconstruct(serialized);
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
        Engine engine = new Engine(grid, engineConfig);
        List<ItemUpdate> updates = schema.evaluate(grid, engine);
        List<ItemUpdate> expected = new ArrayList<>();
        ItemUpdate update1 = new ItemUpdateBuilder(qid1).addStatement(statement1).build();
        expected.add(update1);
        assertEquals(expected, updates);
    }
}
