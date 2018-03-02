package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public class SingleValueScrutinizerTest extends ScrutinizerTest {

    @Override
    public EditScrutinizer getScrutinizer() {
        return new SingleValueScrutinizer();
    }

    @Test
    public void testTrigger() {
        ItemIdValue idA = TestingData.existingId;
        ItemIdValue idB = TestingData.matchedId;
        ItemUpdate update = new ItemUpdateBuilder(idA)
                .addStatement(TestingData.generateStatement(idA, idB))
                .addStatement(TestingData.generateStatement(idA, idB))
                .build();
        scrutinize(update);
        assertWarningsRaised(SingleValueScrutinizer.type);
    }
    
    @Test
    public void testNoIssue() {
        ItemIdValue idA = TestingData.existingId;
        ItemIdValue idB = TestingData.matchedId;
        ItemUpdate updateA = new ItemUpdateBuilder(idA)
                .addStatement(TestingData.generateStatement(idA, idB))
                .build();
        ItemUpdate updateB = new ItemUpdateBuilder(idB)
                .addStatement(TestingData.generateStatement(idB, idB))
                .build();
        scrutinize(updateA, updateB);
        assertNoWarningRaised();
    }
}
