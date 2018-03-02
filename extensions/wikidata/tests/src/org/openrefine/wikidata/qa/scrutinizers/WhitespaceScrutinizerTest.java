package org.openrefine.wikidata.qa.scrutinizers;

import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;

public class WhitespaceScrutinizerTest extends ValueScrutinizerTest {

    @Override
    public EditScrutinizer getScrutinizer() {
        return new WhitespaceScrutinizer();
    }
    
    @Test
    public void testLeadingWhitespace() {
        scrutinize(Datamodel.makeStringValue(" a"));
        assertWarningsRaised(WhitespaceScrutinizer.leadingWhitespaceType);
    }
    
    @Test
    public void testTrailingWhitespace() {
        scrutinize(Datamodel.makeStringValue("a\t"));
        assertWarningsRaised(WhitespaceScrutinizer.trailingWhitespaceType);
    }
    
    @Test
    public void testDuplicateWhitespace() {
        scrutinize(Datamodel.makeStringValue("a\t b"));
        assertWarningsRaised(WhitespaceScrutinizer.duplicateWhitespaceType);
    }

    @Test
    public void testNonPrintableChars() {
        scrutinize(Datamodel.makeStringValue("c\u0003"));
        assertWarningsRaised(WhitespaceScrutinizer.nonPrintableCharsType);
    }
    
    @Test
    public void testNoIssue() {
        scrutinize(Datamodel.makeStringValue("a b"));
        assertNoWarningRaised();
    }
    
    @Test
    public void testMultipleIssues() {
        scrutinize(Datamodel.makeStringValue(" a\t b "));
        assertWarningsRaised(
                WhitespaceScrutinizer.duplicateWhitespaceType,
                WhitespaceScrutinizer.leadingWhitespaceType,
                WhitespaceScrutinizer.trailingWhitespaceType);
    }
    
    @Test
    public void testMonolingualTextValue() {
        scrutinizeLabel(Datamodel.makeMonolingualTextValue(" a", "fr"));
        assertWarningsRaised(WhitespaceScrutinizer.leadingWhitespaceType);
    }
}
