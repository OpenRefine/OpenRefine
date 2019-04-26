package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.MockConstraintFetcher;
import org.openrefine.wikidata.testing.TestingData;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public class EntityTypeScrutinizerTest extends StatementScrutinizerTest {
    
    private static ItemIdValue qid = Datamodel.makeWikidataItemIdValue("Q343");

    @Override
    public EditScrutinizer getScrutinizer() {
        return new EntityTypeScrutinizer();
    }
    
    @Test
    public void testAllowed() {
        scrutinize(TestingData.generateStatement(qid, qid));
        assertNoWarningRaised();
    }

    @Test
    public void testDisallowed() {
        scrutinize(TestingData.generateStatement(qid, MockConstraintFetcher.propertyOnlyPid, qid));
        assertWarningsRaised(EntityTypeScrutinizer.type);
    }
}
