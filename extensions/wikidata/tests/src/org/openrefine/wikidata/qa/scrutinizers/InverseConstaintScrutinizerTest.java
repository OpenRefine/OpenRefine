package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.MockConstraintFetcher;
import org.openrefine.wikidata.testing.TestingDataGenerator;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

public class InverseConstaintScrutinizerTest extends StatementScrutinizerTest {
    
    private ItemIdValue idA = TestingDataGenerator.existingId;
    private ItemIdValue idB = TestingDataGenerator.newIdB;
    private PropertyIdValue pidWithInverse = MockConstraintFetcher.pidWithInverse;
    private PropertyIdValue inversePid = MockConstraintFetcher.inversePid;

    @Override
    public EditScrutinizer getScrutinizer() {
        return new InverseConstraintScrutinizer();
    }
    
    @Test
    public void testTrigger() {
        ItemUpdate update = new ItemUpdateBuilder(idA)
                .addStatement(TestingDataGenerator.generateStatement(idA, pidWithInverse, idB))
                .build();
        scrutinize(update);
        assertWarningsRaised(InverseConstraintScrutinizer.type);
    }
    
    @Test
    public void testNoSymmetricClosure() {
        ItemUpdate update = new ItemUpdateBuilder(idA)
                .addStatement(TestingDataGenerator.generateStatement(idA, inversePid, idB))
                .build();
        scrutinize(update);
        assertNoWarningRaised();
    }

}
