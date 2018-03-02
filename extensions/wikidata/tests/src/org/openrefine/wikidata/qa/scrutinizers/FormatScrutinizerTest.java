package org.openrefine.wikidata.qa.scrutinizers;

import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;

public class FormatScrutinizerTest extends ValueScrutinizerTest {

    @Override
    public EditScrutinizer getScrutinizer() {
        return new FormatScrutinizer();
    }
    
    @Test
    public void testTrigger() {
        scrutinize(Datamodel.makeStringValue("not a number"));
        assertWarningsRaised(FormatScrutinizer.type);
    }
    
    @Test
    public void testNoIssue() {
        scrutinize(Datamodel.makeStringValue("1234"));
        assertNoWarningRaised();
    }
    
    @Test
    public void testIncompleteMatch() {
        scrutinize(Datamodel.makeStringValue("42 is a number"));
        assertWarningsRaised(FormatScrutinizer.type);
    }

}
