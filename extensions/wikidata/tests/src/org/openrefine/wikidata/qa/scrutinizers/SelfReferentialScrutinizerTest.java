package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.testing.TestingDataGenerator;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public class SelfReferentialScrutinizerTest extends StatementScrutinizerTest {

    @Override
    public EditScrutinizer getScrutinizer() {
        return new SelfReferentialScrutinizer();
    }
    
    @Test
    public void testTrigger() {
        ItemIdValue id = TestingDataGenerator.matchedId;
        scrutinize(TestingDataGenerator.generateStatement(id, id));
        assertWarningsRaised(SelfReferentialScrutinizer.type);
    }

    @Test
    public void testNoProblem() {
        ItemIdValue id = TestingDataGenerator.matchedId;
        scrutinize(TestingDataGenerator.generateStatement(id, TestingDataGenerator.existingId));
        assertNoWarningRaised();
    }
}
