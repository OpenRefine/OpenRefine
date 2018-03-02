package org.openrefine.wikidata.qa.scrutinizers;

import java.util.Collections;

import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;


public class NewItemScrutinizerTest extends ScrutinizerTest {
    
    private Claim claim = Datamodel.makeClaim(TestingData.newIdA,
            Datamodel.makeValueSnak(Datamodel.makeWikidataPropertyIdValue("P31"), TestingData.existingId),
                    Collections.emptyList());
    private Statement p31Statement = Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");

    @Override
    public EditScrutinizer getScrutinizer() {
        return new NewItemScrutinizer();
    }
    
    @Test
    public void testTrigger() {
        ItemUpdate update = new ItemUpdateBuilder(TestingData.newIdA).build();
        scrutinize(update);
        assertWarningsRaised(
                NewItemScrutinizer.noDescType,
                NewItemScrutinizer.noLabelType,
                NewItemScrutinizer.noTypeType,
                NewItemScrutinizer.newItemType);
    }
    
    @Test
    public void testEmptyItem() {
        ItemUpdate update = new ItemUpdateBuilder(TestingData.existingId).build();
        scrutinize(update);
        assertNoWarningRaised();
    }
    
    @Test
    public void testGoodNewItem() {
        
        ItemUpdate update = new ItemUpdateBuilder(TestingData.newIdA)
                .addLabel(Datamodel.makeMonolingualTextValue("bonjour", "fr"))
                .addDescription(Datamodel.makeMonolingualTextValue("interesting item", "en"))
                .addStatement(p31Statement)
                .build();
        scrutinize(update);
        assertWarningsRaised(NewItemScrutinizer.newItemType);
    }
    
    @Test
    public void testDeletedStatements() {
        ItemUpdate update = new ItemUpdateBuilder(TestingData.newIdA)
                .addLabel(Datamodel.makeMonolingualTextValue("bonjour", "fr"))
                .addDescription(Datamodel.makeMonolingualTextValue("interesting item", "en"))
                .addStatement(p31Statement)
                .deleteStatement(TestingData.generateStatement(TestingData.newIdA,
                        TestingData.matchedId))
                .build();
        scrutinize(update);
        assertWarningsRaised(NewItemScrutinizer.newItemType, NewItemScrutinizer.deletedStatementsType);
    }

}
