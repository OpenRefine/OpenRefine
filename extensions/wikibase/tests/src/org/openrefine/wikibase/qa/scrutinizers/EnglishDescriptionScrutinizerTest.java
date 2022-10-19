
package org.openrefine.wikibase.qa.scrutinizers;

import org.openrefine.wikibase.testing.TestingData;
import org.openrefine.wikibase.updates.TermedStatementEntityEdit;
import org.openrefine.wikibase.updates.ItemEditBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;

public class EnglishDescriptionScrutinizerTest extends ScrutinizerTest {

    @Override
    public EditScrutinizer getScrutinizer() {
        return new EnglishDescriptionScrutinizer();
    }

    @Test
    public void testGoodDesc() {
        String description = "good description";
        TermedStatementEntityEdit update = new ItemEditBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .build();
        scrutinize(update);
        assertNoWarningRaised();
    }

    @Test
    public void testEndWithPunctuationSign() {
        String description = "description with punctuationSign.";
        TermedStatementEntityEdit update = new ItemEditBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), false)
                .build();
        scrutinize(update);
        assertWarningsRaised(EnglishDescriptionScrutinizer.descEndsByPunctuationSign);
    }

    @Test
    public void testBeginWithUppercase() {
        String description = "Begin with uppercase";
        TermedStatementEntityEdit update = new ItemEditBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .build();
        scrutinize(update);
        assertWarningsRaised(EnglishDescriptionScrutinizer.descBeginWithUppercase);
    }

    @Test
    public void testBeginWithArticle() {
        String description = "an article test";
        TermedStatementEntityEdit update = new ItemEditBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), false)
                .build();
        scrutinize(update);
        assertWarningsRaised(EnglishDescriptionScrutinizer.descBeginWithArticle);
    }

    @Test
    public void testAwfulDesc() {
        String description = "An awful description.";
        TermedStatementEntityEdit update = new ItemEditBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .addLabel(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .build();
        scrutinize(update);
        assertWarningsRaised(EnglishDescriptionScrutinizer.descEndsByPunctuationSign,
                EnglishDescriptionScrutinizer.descBeginWithUppercase, EnglishDescriptionScrutinizer.descBeginWithArticle);
    }
}
