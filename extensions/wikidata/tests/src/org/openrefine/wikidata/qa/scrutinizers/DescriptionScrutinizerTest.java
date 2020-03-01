package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;

public class DescriptionScrutinizerTest extends ScrutinizerTest {

    @Override
    public EditScrutinizer getScrutinizer() {
        return new DescriptionScrutinizer();
    }

    @Test
    public void testTooLong() {
        String description = "long description long description long description long description "
                + "long description long description long description long description "
                + "long description long description long description long description "
                + "long description long description long description long description ";
        ItemUpdate update = new ItemUpdateBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .build();
        scrutinize(update);
        assertWarningsRaised(DescriptionScrutinizer.descTooLongType);
    }

    @Test
    public void testEndWithPunctuationSign() {
        String description = "description with punctuationSign.";
        ItemUpdate update = new ItemUpdateBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), false)
                .build();
        scrutinize(update);
        assertWarningsRaised(DescriptionScrutinizer.descEndsByPunctuationSign);
    }

    @Test
    public void testBeginWithUppercase() {
        String description = "Begin with uppercase";
        ItemUpdate update = new ItemUpdateBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .build();
        scrutinize(update);
        assertWarningsRaised(DescriptionScrutinizer.descBeginWithUppercase);
    }

    @Test
    public void testBeginWithArticle() {
        String description = "an article test";
        ItemUpdate update = new ItemUpdateBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), false)
                .build();
        scrutinize(update);
        assertWarningsRaised(DescriptionScrutinizer.descBeginWithArticle);
    }

    @Test
    public void testIdenticalWithLabel() {
        String description = "identical with label";
        ItemUpdate update = new ItemUpdateBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .addLabel(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .build();
        scrutinize(update);
        assertWarningsRaised(DescriptionScrutinizer.descIdenticalWithLabel);
    }

    @Test
    public void testIdenticalWithLabel1() {
        String description = "identical with label";
        ItemUpdate update = new ItemUpdateBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .addLabel(Datamodel.makeMonolingualTextValue("bonjour", "fr"), true)
                .build();
        scrutinize(update);
        assertNoWarningRaised();
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
    public void testAwfulDesc() {
        String description = "An awful description An awful description An awful description An awful description"
                + "An awful description An awful description An awful description An awful description"
                + "An awful description An awful description An awful description An awful description"
                + "An awful description An awful description An awful description An awful description!";
        ItemUpdate update = new ItemUpdateBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .addLabel(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .build();
        scrutinize(update);
        assertWarningsRaised(DescriptionScrutinizer.descTooLongType, DescriptionScrutinizer.descEndsByPunctuationSign,
                DescriptionScrutinizer.descBeginWithUppercase, DescriptionScrutinizer.descBeginWithArticle, DescriptionScrutinizer.descIdenticalWithLabel);
    }
}
