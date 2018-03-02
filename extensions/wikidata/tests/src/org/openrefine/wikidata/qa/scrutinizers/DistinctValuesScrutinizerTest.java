package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.testing.TestingDataGenerator;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public class DistinctValuesScrutinizerTest extends StatementScrutinizerTest {

    @Override
    public EditScrutinizer getScrutinizer() {
        return new DistinctValuesScrutinizer();
    }

    @Test
    public void testTrigger() {
        ItemIdValue idA = TestingDataGenerator.existingId;
        ItemIdValue idB = TestingDataGenerator.matchedId;
        ItemUpdate updateA = new ItemUpdateBuilder(idA)
                .addStatement(TestingDataGenerator.generateStatement(idA, idB))
                .build();
        ItemUpdate updateB = new ItemUpdateBuilder(idB)
                .addStatement(TestingDataGenerator.generateStatement(idB, idB))
                .build();
        scrutinize(updateA, updateB);
        assertWarningsRaised(DistinctValuesScrutinizer.type);
    }
}
