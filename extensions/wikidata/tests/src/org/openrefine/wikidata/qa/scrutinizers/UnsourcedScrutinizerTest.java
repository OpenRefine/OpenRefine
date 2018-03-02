package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.testing.TestingData;
import org.testng.annotations.Test;

public class UnsourcedScrutinizerTest extends StatementScrutinizerTest {

    @Override
    public EditScrutinizer getScrutinizer() {
        return new UnsourcedScrutinizer();
    }
    
    @Test
    public void testTrigger() {
        scrutinize(TestingData.generateStatement(TestingData.existingId,
                TestingData.matchedId));
        assertWarningsRaised(UnsourcedScrutinizer.type);
    }

}
