package org.openrefine.wikidata.qa.scrutinizers;

import java.util.Collections;
import java.util.List;

import org.openrefine.wikidata.qa.MockConstraintFetcher;
import org.openrefine.wikidata.testing.TestingData;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

public class RestrictedPositionScrutinizerTest extends SnakScrutinizerTest {
    
    private ItemIdValue qid = TestingData.existingId;

    @Override
    public EditScrutinizer getScrutinizer() {
        return new RestrictedPositionScrutinizer();
    }
    
    @Test
    public void testTriggerMainSnak() {
        scrutinize(TestingData.generateStatement(qid, MockConstraintFetcher.qualifierPid, qid));
        assertWarningsRaised("property-restricted-to-qualifier-found-in-mainsnak");
    }
    
    @Test
    public void testNoProblem() {
        scrutinize(TestingData.generateStatement(qid, MockConstraintFetcher.mainSnakPid, qid));
        assertNoWarningRaised();
    }
    
    @Test
    public void testNotRestricted() {
        scrutinize(TestingData.generateStatement(qid, Datamodel.makeWikidataPropertyIdValue("P3748"), qid));
        assertNoWarningRaised();
    }
    
    @Test
    public void testTriggerReference() {
        Snak snak = Datamodel.makeValueSnak(MockConstraintFetcher.mainSnakPid, qid);
        List<SnakGroup> snakGroups = Collections.singletonList(Datamodel.makeSnakGroup(Collections.singletonList(snak)));
        Statement statement = Datamodel.makeStatement(
                TestingData.generateStatement(qid, MockConstraintFetcher.mainSnakPid, qid).getClaim(),
                Collections.singletonList(Datamodel.makeReference(snakGroups)),
                StatementRank.NORMAL, "");
        scrutinize(statement);
        assertWarningsRaised("property-restricted-to-mainsnak-found-in-reference");
    }

}
