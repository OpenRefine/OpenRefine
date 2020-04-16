package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
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
        ItemUpdate update = new ItemUpdateBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .build();
        scrutinize(update);
        assertNoWarningRaised();
    }

    @Test
    public void testEndWithPunctuationSign() {
        String description = "description with punctuationSign.";
        ItemUpdate update = new ItemUpdateBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), false)
                .build();
        scrutinize(update);
        assertWarningsRaised(EnglishDescriptionScrutinizer.descEndsByPunctuationSign);
    }

    @Test
    public void testBeginWithUppercase() {
        String description = "Begin with uppercase";
        ItemUpdate update = new ItemUpdateBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .build();
        scrutinize(update);
        assertWarningsRaised(EnglishDescriptionScrutinizer.descBeginWithUppercase);
    }

    @Test
    public void testBeginWithArticle() {
        String description = "an article test";
        ItemUpdate update = new ItemUpdateBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), false)
                .build();
        scrutinize(update);
        assertWarningsRaised(EnglishDescriptionScrutinizer.descBeginWithArticle);
    }

    @Test
    public void testAwfulDesc() {
        String description = "An awful description.";
        ItemUpdate update = new ItemUpdateBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .addLabel(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .build();
        scrutinize(update);
        assertWarningsRaised(EnglishDescriptionScrutinizer.descEndsByPunctuationSign,
                EnglishDescriptionScrutinizer.descBeginWithUppercase, EnglishDescriptionScrutinizer.descBeginWithArticle);
    }
}
