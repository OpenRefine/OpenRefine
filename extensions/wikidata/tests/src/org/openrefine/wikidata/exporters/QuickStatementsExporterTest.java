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
package org.openrefine.wikidata.exporters;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.openrefine.wikidata.schema.WikibaseSchema;
import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.testing.WikidataRefineTest;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;

public class QuickStatementsExporterTest extends WikidataRefineTest {

    private QuickStatementsExporter exporter = new QuickStatementsExporter();
    private ItemIdValue newIdA = TestingData.newIdA;
    private ItemIdValue newIdB = TestingData.newIdB;
    private ItemIdValue qid1 = Datamodel.makeWikidataItemIdValue("Q1377");
    private ItemIdValue qid2 = Datamodel.makeWikidataItemIdValue("Q865528");

    private String export(ItemUpdate... itemUpdates)
            throws IOException {
        StringWriter writer = new StringWriter();
        exporter.translateItemList(Arrays.asList(itemUpdates), writer);
        return writer.toString();
    }

    @Test
    public void testSimpleProject()
            throws IOException {
        Project project = this.createCSVProject(TestingData.inceptionWithNewCsv);
        TestingData.reconcileInceptionCells(project);
        String serialized = TestingData.jsonFromFile("schema/inception.json");
        WikibaseSchema schema = WikibaseSchema.reconstruct(serialized);
        project.overlayModels.put("wikibaseSchema", schema);
        Engine engine = new Engine(project);

        StringWriter writer = new StringWriter();
        Properties properties = new Properties();
        exporter.export(project, properties, engine, writer);
        assertEquals(TestingData.inceptionWithNewQS, writer.toString());
    }

    @Test
    public void testImpossibleScheduling()
            throws IOException {
        Statement sNewAtoNewB = TestingData.generateStatement(newIdA, newIdB);
        ItemUpdate update = new ItemUpdateBuilder(newIdA).addStatement(sNewAtoNewB).build();

        assertEquals(QuickStatementsExporter.impossibleSchedulingErrorMessage, export(update));
    }

    @Test
    public void testNameDesc()
            throws IOException {
    	/**
    	 * Adding labels and description without overriding is not supported by QS, so
    	 * we fall back on adding them with overriding.
    	 */
        ItemUpdate update = new ItemUpdateBuilder(qid1)
                .addLabel(Datamodel.makeMonolingualTextValue("some label", "en"), true)
                .addDescription(Datamodel.makeMonolingualTextValue("some description", "en"), true)
                .build();

        assertEquals("Q1377\tLen\t\"some label\"\n" + "Q1377\tDen\t\"some description\"\n", export(update));
    }
    
    @Test
    public void testOptionalNameDesc()
            throws IOException {
        ItemUpdate update = new ItemUpdateBuilder(newIdA)
                .addLabel(Datamodel.makeMonolingualTextValue("my new item", "en"), false)
                .addDescription(Datamodel.makeMonolingualTextValue("isn't it awesome?", "en"), false)
                .addAlias(Datamodel.makeMonolingualTextValue("fabitem", "en")).build();

        assertEquals("CREATE\n" + "LAST\tLen\t\"my new item\"\n" + "LAST\tDen\t\"isn't it awesome?\"\n"
                + "LAST\tAen\t\"fabitem\"\n", export(update));
    }

    @Test
    public void testDeleteStatement()
            throws IOException {
        ItemUpdate update = new ItemUpdateBuilder(qid1).deleteStatement(TestingData.generateStatement(qid1, qid2))
                .build();

        assertEquals("- Q1377\tP38\tQ865528\n", export(update));
    }

    @Test
    public void testQualifier()
            throws IOException {
        Statement baseStatement = TestingData.generateStatement(qid1, qid2);
        Statement otherStatement = TestingData.generateStatement(qid2, qid1);
        Snak qualifierSnak = otherStatement.getClaim().getMainSnak();
        SnakGroup group = Datamodel.makeSnakGroup(Collections.singletonList(qualifierSnak));
        Claim claim = Datamodel.makeClaim(qid1, baseStatement.getClaim().getMainSnak(),
                Collections.singletonList(group));
        Statement statement = Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");
        ItemUpdate update = new ItemUpdateBuilder(qid1).addStatement(statement).build();

        assertEquals("Q1377\tP38\tQ865528\tP38\tQ1377\n", export(update));
    }

    @Test
    public void testNoSchema()
            throws IOException {
        Project project = this.createCSVProject("a,b\nc,d");
        Engine engine = new Engine(project);
        StringWriter writer = new StringWriter();
        Properties properties = new Properties();
        exporter.export(project, properties, engine, writer);
        assertEquals(QuickStatementsExporter.noSchemaErrorMessage, writer.toString());
    }

    @Test
    public void testGetContentType() {
        assertEquals("text/plain", exporter.getContentType());
    }
}
