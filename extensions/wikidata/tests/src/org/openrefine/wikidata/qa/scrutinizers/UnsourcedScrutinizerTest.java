package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.testing.TestingDataGenerator;
import org.testng.annotations.Test;

public class UnsourcedScrutinizerTest extends StatementScrutinizerTest {

    @Override
    public EditScrutinizer getScrutinizer() {
        return new UnsourcedScrutinizer();
    }
    
    @Test
    public void testTrigger() {
        scrutinize(TestingDataGenerator.generateStatement(TestingDataGenerator.existingId,
                TestingDataGenerator.matchedId));
        assertWarningsRaised(UnsourcedScrutinizer.type);
    }

}
