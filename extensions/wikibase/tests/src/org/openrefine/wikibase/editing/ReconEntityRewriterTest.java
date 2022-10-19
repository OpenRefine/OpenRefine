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

package org.openrefine.wikibase.editing;

import static org.testng.Assert.assertEquals;

import org.openrefine.wikibase.schema.exceptions.NewEntityNotCreatedYetException;
import org.openrefine.wikibase.testing.TestingData;
import org.openrefine.wikibase.updates.LabeledStatementEntityEdit;
import org.openrefine.wikibase.updates.TermedStatementEntityEdit;
import org.openrefine.wikibase.updates.EntityEdit;
import org.openrefine.wikibase.updates.ItemEditBuilder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

public class ReconEntityRewriterTest {

    NewEntityLibrary library = null;
    ReconEntityRewriter rewriter = null;
    ItemIdValue newlyCreated = Datamodel.makeWikidataItemIdValue("Q1234");
    PropertyIdValue newlyCreatedProperty = Datamodel.makeWikidataPropertyIdValue("P1234");

    @BeforeMethod
    public void setUp() {
        library = new NewEntityLibrary();
    }

    @Test(expectedExceptions = ReconEntityRewriter.MissingEntityIdFound.class)
    public void testNotCreatedYet() {
        rewriter = new ReconEntityRewriter(library, TestingData.newIdA);
        rewriter.copy(TestingData.newIdB);
    }

    @Test
    public void testSuccessfulRewrite() {
        rewriter = new ReconEntityRewriter(library, TestingData.newIdA);
        library.setId(4567L, "Q1234");
        assertEquals(newlyCreated, rewriter.copy(TestingData.newIdB));
    }

    @Test
    public void testSubjectNotRewritten() {
        ItemIdValue subject = TestingData.newIdA;
        rewriter = new ReconEntityRewriter(library, subject);
        assertEquals(subject, rewriter.copy(subject));
    }

    @Test
    public void testSubjectRewritten() {
        ItemIdValue subject = TestingData.newIdB;
        library.setId(4567L, "Q1234");
        rewriter = new ReconEntityRewriter(library, subject);
        assertEquals(newlyCreated, rewriter.copy(subject));
    }

    @Test
    public void testMatched() {
        rewriter = new ReconEntityRewriter(library, TestingData.newIdA);
        assertEquals(TestingData.matchedId, rewriter.copy(TestingData.matchedId));
    }

    @Test
    public void testRewriteCreate() throws NewEntityNotCreatedYetException {
        ItemIdValue subject = TestingData.newIdA;
        rewriter = new ReconEntityRewriter(library, subject);
        library.setId(4567L, "Q1234");
        TermedStatementEntityEdit update = new ItemEditBuilder(subject)
                .addStatement(TestingData.generateStatementAddition(subject, TestingData.newIdB))
                .addStatement(TestingData.generateStatementDeletion(subject, TestingData.existingId))
                .addLabel(Datamodel.makeMonolingualTextValue("label", "de"), true)
                .addDescription(Datamodel.makeMonolingualTextValue("beschreibung", "de"), false)
                .addAlias(Datamodel.makeMonolingualTextValue("darstellung", "de")).build();
        EntityEdit rewritten = rewriter.rewrite(update);
        LabeledStatementEntityEdit expected = new ItemEditBuilder(subject)
                .addStatement(TestingData.generateStatementAddition(subject, newlyCreated))
                .addStatement(TestingData.generateStatementDeletion(subject, TestingData.existingId))
                .addLabel(Datamodel.makeMonolingualTextValue("label", "de"), true)
                .addDescription(Datamodel.makeMonolingualTextValue("beschreibung", "de"), false)
                .addAlias(Datamodel.makeMonolingualTextValue("darstellung", "de")).build();
        assertEquals(rewritten, expected);
    }

    @Test
    public void testRewriteUpdateOnPreviouslyCreatedEntity() throws NewEntityNotCreatedYetException {
        ItemIdValue subject = TestingData.newIdA;
        rewriter = new ReconEntityRewriter(library, subject);
        library.setId(4567L, "Q1234");
        TermedStatementEntityEdit update = new ItemEditBuilder(TestingData.newIdB)
                .addDescription(Datamodel.makeMonolingualTextValue("beschreibung", "de"), false)
                .addAlias(Datamodel.makeMonolingualTextValue("darstellung", "de")).build();
        EntityEdit rewritten = rewriter.rewrite(update);
        LabeledStatementEntityEdit expected = new ItemEditBuilder(newlyCreated)
                .addDescription(Datamodel.makeMonolingualTextValue("beschreibung", "de"), false)
                .addAlias(Datamodel.makeMonolingualTextValue("darstellung", "de")).build();
        assertEquals(rewritten, expected);
    }

    @Test
    public void testRewriteUpdateOnExistingEntity() throws NewEntityNotCreatedYetException {
        ItemIdValue subject = TestingData.matchedId;
        rewriter = new ReconEntityRewriter(library, subject);
        library.setId(4567L, "Q1234");
        TermedStatementEntityEdit update = new ItemEditBuilder(subject)
                .addStatement(TestingData.generateStatementAddition(subject, TestingData.newIdB))
                .addStatement(TestingData.generateStatementDeletion(subject, TestingData.existingId))
                .addLabel(Datamodel.makeMonolingualTextValue("label", "de"), true)
                .addDescription(Datamodel.makeMonolingualTextValue("beschreibung", "de"), false)
                .addAlias(Datamodel.makeMonolingualTextValue("darstellung", "de")).build();
        EntityEdit rewritten = rewriter.rewrite(update);
        LabeledStatementEntityEdit expected = new ItemEditBuilder(subject)
                .addStatement(TestingData.generateStatementAddition(subject, newlyCreated))
                .addStatement(TestingData.generateStatementDeletion(subject, TestingData.existingId))
                .addLabel(Datamodel.makeMonolingualTextValue("label", "de"), true)
                .addDescription(Datamodel.makeMonolingualTextValue("beschreibung", "de"), false)
                .addAlias(Datamodel.makeMonolingualTextValue("darstellung", "de")).build();
        assertEquals(rewritten, expected);
    }

    @Test
    public void testRewritePropertyUpdateOnExistingEntity() throws NewEntityNotCreatedYetException {
        ItemIdValue subject = TestingData.existingId;
        rewriter = new ReconEntityRewriter(library, subject);
        library.setId(7654L, "P1234");
        TermedStatementEntityEdit update = new ItemEditBuilder(subject)
                .addStatement(TestingData.generateStatementAddition(subject, TestingData.newPropertyIdB))
                .addStatement(TestingData.generateStatementDeletion(subject, TestingData.existingPropertyId))
                .addLabel(Datamodel.makeMonolingualTextValue("label", "de"), true)
                .addDescription(Datamodel.makeMonolingualTextValue("beschreibung", "de"), false)
                .addAlias(Datamodel.makeMonolingualTextValue("darstellung", "de")).build();
        EntityEdit rewritten = rewriter.rewrite(update);
        LabeledStatementEntityEdit expected = new ItemEditBuilder(subject)
                .addStatement(TestingData.generateStatementAddition(subject, newlyCreatedProperty))
                .addStatement(TestingData.generateStatementDeletion(subject, TestingData.existingPropertyId))
                .addLabel(Datamodel.makeMonolingualTextValue("label", "de"), true)
                .addDescription(Datamodel.makeMonolingualTextValue("beschreibung", "de"), false)
                .addAlias(Datamodel.makeMonolingualTextValue("darstellung", "de")).build();
        assertEquals(rewritten, expected);
    }
}
