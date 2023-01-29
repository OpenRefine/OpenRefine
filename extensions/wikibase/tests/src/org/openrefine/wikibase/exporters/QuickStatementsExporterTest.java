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

package org.openrefine.wikibase.exporters;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

import org.openrefine.ProjectMetadata;
import org.openrefine.RefineTest;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.Grid;
import org.openrefine.wikibase.schema.WikibaseSchema;
import org.openrefine.wikibase.schema.strategies.StatementEditingMode;
import org.openrefine.wikibase.schema.strategies.StatementMerger;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.openrefine.wikibase.testing.TestingData;
import org.openrefine.wikibase.updates.ItemEditBuilder;
import org.openrefine.wikibase.updates.StatementEdit;
import org.openrefine.wikibase.updates.TermedStatementEntityEdit;

public class QuickStatementsExporterTest extends RefineTest {

    private QuickStatementsExporter exporter = new QuickStatementsExporter();
    private ItemIdValue newIdA = TestingData.newIdA;
    private ItemIdValue newIdB = TestingData.newIdB;
    private ItemIdValue qid1 = Datamodel.makeWikidataItemIdValue("Q1377");
    private ItemIdValue qid2 = Datamodel.makeWikidataItemIdValue("Q865528");

    private String export(TermedStatementEntityEdit... itemUpdates) throws IOException {
        StringWriter writer = new StringWriter();
        exporter.translateItemList(Arrays.asList(itemUpdates), writer);
        return writer.toString();
    }

    @Test
    public void testSimpleProject()
            throws IOException {
        Grid grid = createGrid(
                new String[] { "subject", "inception", "reference" },
                new Serializable[][] {
                        { TestingData.makeMatchedCell("Q1377", "University of Ljubljana"), "1919",
                                "http://www.ljubljana-slovenia.com/university-ljubljana" },
                        { TestingData.makeMatchedCell("Q865528", "University of Warwick"), "1965", "" },
                        { TestingData.makeNewItemCell(1234L, "new uni"), "2016", "http://new-uni.com/" } });
        String serialized = TestingData.jsonFromFile("schema/inception.json");
        WikibaseSchema schema = WikibaseSchema.reconstruct(serialized);
        schema.validate(new ValidationState(grid.getColumnModel()));
        grid = grid.withOverlayModels(Collections.singletonMap("wikibaseSchema", schema));
        Engine engine = new Engine(grid, EngineConfig.ALL_ROWS);

        StringWriter writer = new StringWriter();
        Properties properties = new Properties();
        exporter.export(grid, new ProjectMetadata(), properties, engine, writer);
        assertEquals(writer.toString(), TestingData.inceptionWithNewQS);
    }

    @Test
    public void testImpossibleScheduling() throws IOException {
        StatementEdit sNewAtoNewB = TestingData.generateStatementAddition(newIdA, newIdB);
        TermedStatementEntityEdit update = new ItemEditBuilder(newIdA).addStatement(sNewAtoNewB)
                .build();

        assertEquals(QuickStatementsExporter.impossibleSchedulingErrorMessage, export(update));
    }

    @Test
    public void testNameDesc() throws IOException {
        /**
         * Adding labels and description without overriding is not supported by QS, so we fall back on adding them with
         * overriding.
         */
        TermedStatementEntityEdit update = new ItemEditBuilder(qid1)
                .addLabel(Datamodel.makeMonolingualTextValue("some label", "en"), true)
                .addDescription(Datamodel.makeMonolingualTextValue("some description", "en"), true).build();

        assertEquals("Q1377\tLen\t\"some label\"\n" + "Q1377\tDen\t\"some description\"\n", export(update));
    }

    @Test
    public void testOptionalNameDesc() throws IOException {
        TermedStatementEntityEdit update = new ItemEditBuilder(newIdA)
                .addLabel(Datamodel.makeMonolingualTextValue("my new item", "en"), false)
                .addDescription(Datamodel.makeMonolingualTextValue("isn't it awesome?", "en"), false)
                .addAlias(Datamodel.makeMonolingualTextValue("fabitem", "en")).build();

        assertEquals("CREATE\n" + "LAST\tLen\t\"my new item\"\n" + "LAST\tDen\t\"isn't it awesome?\"\n"
                + "LAST\tAen\t\"fabitem\"\n", export(update));
    }

    @Test
    public void testDeleteStatement() throws IOException {
        TermedStatementEntityEdit update = new ItemEditBuilder(qid1)
                .addStatement(new StatementEdit(TestingData.generateStatement(qid1, qid2),
                        StatementMerger.FORMER_DEFAULT_STRATEGY, StatementEditingMode.DELETE))
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
        StatementEdit statementUpdate = new StatementEdit(statement, StatementMerger.FORMER_DEFAULT_STRATEGY,
                StatementEditingMode.ADD_OR_MERGE);
        TermedStatementEntityEdit update = new ItemEditBuilder(qid1).addStatement(statementUpdate).build();

        assertEquals("Q1377\tP38\tQ865528\tP38\tQ1377\n", export(update));
    }

    @Test
    public void testSomeValue() throws IOException {
        PropertyIdValue pid = Datamodel.makeWikidataPropertyIdValue("P123");
        Claim claim = Datamodel.makeClaim(qid1, Datamodel.makeSomeValueSnak(pid), Collections.emptyList());
        Statement statement = Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");
        StatementEdit statementUpdate = new StatementEdit(statement, StatementMerger.FORMER_DEFAULT_STRATEGY,
                StatementEditingMode.ADD_OR_MERGE);

        TermedStatementEntityEdit update = new ItemEditBuilder(qid1).addStatement(statementUpdate)
                .build();

        assertEquals("Q1377\tP123\tsomevalue\n", export(update));
    }

    @Test
    public void testNoValue() throws IOException {
        PropertyIdValue pid = Datamodel.makeWikidataPropertyIdValue("P123");
        Claim claim = Datamodel.makeClaim(qid1, Datamodel.makeNoValueSnak(pid), Collections.emptyList());
        Statement statement = Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");
        StatementEdit statementUpdate = new StatementEdit(statement, StatementMerger.FORMER_DEFAULT_STRATEGY,
                StatementEditingMode.ADD_OR_MERGE);

        TermedStatementEntityEdit update = new ItemEditBuilder(qid1).addStatement(statementUpdate)
                .build();

        assertEquals("Q1377\tP123\tnovalue\n", export(update));
    }

    /**
     * issue #2320
     *
     * A statement with different references should be duplicated, but each with a different reference.
     */
    @Test
    public void testReferences() throws IOException {
        Statement baseStatement = TestingData.generateStatement(qid1, qid2);
        Statement otherStatement = TestingData.generateStatement(qid2, qid1);

        Snak snak1 = baseStatement.getClaim().getMainSnak();
        Snak snak2 = otherStatement.getClaim().getMainSnak();
        SnakGroup group1 = Datamodel.makeSnakGroup(Collections.singletonList(snak1));
        SnakGroup group2 = Datamodel.makeSnakGroup(Collections.singletonList(snak2));
        Claim claim = Datamodel.makeClaim(qid1, baseStatement.getClaim().getMainSnak(),
                Collections.singletonList(group2));

        Reference reference1 = Datamodel.makeReference(Collections.singletonList(group1));
        Reference reference2 = Datamodel.makeReference(Collections.singletonList(group2));

        Statement statement = Datamodel.makeStatement(claim, Arrays.asList(reference1, reference2),
                StatementRank.NORMAL, "");
        StatementEdit statementUpdate = new StatementEdit(statement, StatementMerger.FORMER_DEFAULT_STRATEGY,
                StatementEditingMode.ADD_OR_MERGE);

        TermedStatementEntityEdit update = new ItemEditBuilder(qid1).addStatement(statementUpdate)
                .build();

        assertEquals(
                "Q1377\tP38\tQ865528\tP38\tQ1377\tS38\tQ865528\n" + "Q1377\tP38\tQ865528\tP38\tQ1377\tS38\tQ1377\n",
                export(update));
    }

    @Test
    public void testNoSchema()
            throws IOException {
        Grid grid = this.createGrid(
                new String[] { "a", "b" },
                new Serializable[][] { { "c", "d" } });
        Engine engine = new Engine(grid, EngineConfig.ALL_ROWS);
        StringWriter writer = new StringWriter();
        Properties properties = new Properties();
        exporter.export(grid, new ProjectMetadata(), properties, engine, writer);
        assertEquals(QuickStatementsExporter.noSchemaErrorMessage, writer.toString());
    }

    @Test
    public void testGetContentType() {
        assertEquals("text/plain", exporter.getContentType());
    }
}
