package org.openrefine.wikidata.editing;

import static org.junit.Assert.assertEquals;

import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public class ReconEntityRewriterTest {
    
    NewItemLibrary library = null;
    ReconEntityRewriter rewriter = null;
    ItemIdValue subject = TestingData.newIdA;
    ItemIdValue newlyCreated = Datamodel.makeWikidataItemIdValue("Q1234");
    
    @BeforeMethod
    public void setUp() {
        library = new NewItemLibrary();
        rewriter = new ReconEntityRewriter(library, subject);
    }
    
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testNotCreatedYet() {
        rewriter.copy(TestingData.newIdB);
    }
    
    @Test
    public void testSuccessfulRewrite() {
        library.setQid(4567L, "Q1234");
        assertEquals(newlyCreated, rewriter.copy(TestingData.newIdB));
    }
    
    @Test
    public void testSubjectNotRewriten() {
        assertEquals(subject, rewriter.copy(subject));
    }
    
    @Test
    public void testMatched() {
        assertEquals(TestingData.matchedId, rewriter.copy(TestingData.matchedId));
    }
    
    @Test
    public void testRewriteUpdate() {
        library.setQid(4567L, "Q1234");
        ItemUpdate update = new ItemUpdateBuilder(subject)
                .addStatement(TestingData.generateStatement(subject, TestingData.newIdB))
                .deleteStatement(TestingData.generateStatement(subject, TestingData.existingId))
                .addLabel(Datamodel.makeMonolingualTextValue("label", "de"))
                .addDescription(Datamodel.makeMonolingualTextValue("beschreibung", "de"))
                .addAlias(Datamodel.makeMonolingualTextValue("darstellung", "de"))
                .build();
        ItemUpdate rewritten = rewriter.rewrite(update);
        ItemUpdate expected = new ItemUpdateBuilder(subject)
                .addStatement(TestingData.generateStatement(subject, newlyCreated))
                .deleteStatement(TestingData.generateStatement(subject, TestingData.existingId))
                .addLabel(Datamodel.makeMonolingualTextValue("label", "de"))
                .addDescription(Datamodel.makeMonolingualTextValue("beschreibung", "de"))
                .addAlias(Datamodel.makeMonolingualTextValue("darstellung", "de"))
                .build();
        assertEquals(expected, rewritten);
    }
}
