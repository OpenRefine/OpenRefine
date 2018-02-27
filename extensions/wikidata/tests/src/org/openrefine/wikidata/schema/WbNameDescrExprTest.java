package org.openrefine.wikidata.schema;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.openrefine.wikidata.testing.TestingDataGenerator;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

public class WbNameDescrExprTest extends WbExpressionTest<MonolingualTextValue> {
    
    @Test
    public void testContributeToLabel() {
        WbNameDescExpr labelExpr = new WbNameDescExpr(WbNameDescExpr.NameDescrType.LABEL,
                TestingDataGenerator.getTestMonolingualExpr("fr", "français", "le croissant magnifique"));
        ItemUpdate update = new ItemUpdate(Datamodel.makeWikidataItemIdValue("Q56"));
        labelExpr.contributeTo(update, ctxt);
        assertEquals(Collections.singletonList(Datamodel.makeMonolingualTextValue("le croissant magnifique", "fr")), update.getLabels());
    }

    @Test
    public void testContributeToDescription() {
        WbNameDescExpr descriptionExpr = new WbNameDescExpr(WbNameDescExpr.NameDescrType.DESCRIPTION,
                TestingDataGenerator.getTestMonolingualExpr("de", "Deutsch", "wunderschön"));
        ItemUpdate update = new ItemUpdate(Datamodel.makeWikidataItemIdValue("Q56"));
        descriptionExpr.contributeTo(update, ctxt);
        assertEquals(Collections.singletonList(Datamodel.makeMonolingualTextValue("wunderschön", "de")), update.getDescriptions());
    }
    
    @Test
    public void testContributeToAlias() {
        WbNameDescExpr aliasExpr = new WbNameDescExpr(WbNameDescExpr.NameDescrType.ALIAS,
                TestingDataGenerator.getTestMonolingualExpr("en", "English", "snack"));
        ItemUpdate update = new ItemUpdate(Datamodel.makeWikidataItemIdValue("Q56"));
        aliasExpr.contributeTo(update, ctxt);
        assertEquals(Collections.singletonList(Datamodel.makeMonolingualTextValue("snack", "en")), update.getAliases());
    }
    
    @Test
    public void testGetters() {
        WbMonolingualExpr monolingualExpr = TestingDataGenerator.getTestMonolingualExpr("en", "English", "not sure what");
        WbNameDescExpr aliasExpr = new WbNameDescExpr(WbNameDescExpr.NameDescrType.ALIAS,
                monolingualExpr);
        assertEquals(WbNameDescExpr.NameDescrType.ALIAS, aliasExpr.getType());
        assertEquals(monolingualExpr, aliasExpr.getValue());
    }
}
