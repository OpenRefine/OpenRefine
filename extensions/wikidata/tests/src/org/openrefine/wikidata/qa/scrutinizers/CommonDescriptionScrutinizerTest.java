package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;

public class CommonDescriptionScrutinizerTest extends ScrutinizerTest {

    @Override
    public EditScrutinizer getScrutinizer() {
        return new CommonDescriptionScrutinizer();
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
    public void testTooLong() {
        String description = "long description long description long description long description "
                + "long description long description long description long description "
                + "long description long description long description long description "
                + "long description long description long description long description";
        ItemUpdate update = new ItemUpdateBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .build();
        scrutinize(update);
        assertWarningsRaised(CommonDescriptionScrutinizer.descTooLongType);
    }

    @Test
    public void testIdenticalWithLabel() {
        String description = "identical with label";
        ItemUpdate update = new ItemUpdateBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .addLabel(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .build();
        scrutinize(update);
        assertWarningsRaised(CommonDescriptionScrutinizer.descIdenticalWithLabel);
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
    public void testAwfulDesc() {
        String description = "long description long description long description long description "
                + "long description long description long description long description "
                + "long description long description long description long description "
                + "long description long description long description long description";
        ItemUpdate update = new ItemUpdateBuilder(TestingData.newIdA)
                .addDescription(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .addLabel(Datamodel.makeMonolingualTextValue(description, "en"), true)
                .build();
        scrutinize(update);
        assertWarningsRaised(CommonDescriptionScrutinizer.descTooLongType, CommonDescriptionScrutinizer.descIdenticalWithLabel);
    }
}
