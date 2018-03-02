package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.testng.annotations.Test;

public class NoEditsMadeScrutinizerTest extends ScrutinizerTest {

    @Override
    public EditScrutinizer getScrutinizer() {
        return new NoEditsMadeScrutinizer();
    }
    
    @Test
    public void testTrigger() {
        scrutinize();
        assertWarningsRaised(NoEditsMadeScrutinizer.type);
    }
    
    @Test
    public void testNonNull() {
        scrutinize(new ItemUpdateBuilder(TestingData.newIdA).build());
        assertNoWarningRaised();
    }
    
    @Test
    public void testNull() {
        scrutinize(new ItemUpdateBuilder(TestingData.existingId).build());
        assertWarningsRaised(NoEditsMadeScrutinizer.type);
    }
}
