package org.openrefine.wikidata.schema;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.openrefine.wikidata.testing.TestingDataGenerator;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

public class WbNameDescExprTest extends WbExpressionTest<MonolingualTextValue> {
    private ItemIdValue subject = Datamodel.makeWikidataItemIdValue("Q56");
    public WbNameDescExpr expr = new WbNameDescExpr(WbNameDescExpr.NameDescrType.ALIAS,
            new WbMonolingualExpr(new WbLanguageConstant("en", "English"),
                 new WbStringVariable("column A")));
    
    public String jsonRepresentation = "{\"name_type\":\"ALIAS\",\"value\":{\"type\":\"wbmonolingualexpr\",\"language\":"+
              "{\"type\":\"wblanguageconstant\",\"id\":\"en\",\"label\":\"English\"},\"value\":"+
            "{\"type\":\"wbstringvariable\",\"columnName\":\"column A\"}}}";
    
    @Test
    public void testContributeToLabel() {
        WbNameDescExpr labelExpr = new WbNameDescExpr(WbNameDescExpr.NameDescrType.LABEL,
                TestingDataGenerator.getTestMonolingualExpr("fr", "français", "le croissant magnifique"));
        ItemUpdate update = new ItemUpdate(subject);
        labelExpr.contributeTo(update, ctxt);
        assertEquals(Collections.singleton(Datamodel.makeMonolingualTextValue("le croissant magnifique", "fr")),
                update.getLabels());
    }

    @Test
    public void testContributeToDescription() {
        WbNameDescExpr descriptionExpr = new WbNameDescExpr(WbNameDescExpr.NameDescrType.DESCRIPTION,
                TestingDataGenerator.getTestMonolingualExpr("de", "Deutsch", "wunderschön"));
        ItemUpdate update = new ItemUpdate(subject);
        descriptionExpr.contributeTo(update, ctxt);
        assertEquals(Collections.singleton(Datamodel.makeMonolingualTextValue("wunderschön", "de")),
                update.getDescriptions());
    }
    
    @Test
    public void testContributeToAlias() {
        WbNameDescExpr aliasExpr = new WbNameDescExpr(WbNameDescExpr.NameDescrType.ALIAS,
                TestingDataGenerator.getTestMonolingualExpr("en", "English", "snack"));
        ItemUpdate update = new ItemUpdate(subject);
        aliasExpr.contributeTo(update, ctxt);
        assertEquals(Collections.singleton(Datamodel.makeMonolingualTextValue("snack", "en")),
                update.getAliases());
    }
    
    @Test
    public void testSkipped() {
        ItemUpdate update = new ItemUpdate(subject);
        setRow("");
        expr.contributeTo(update, ctxt);
        assertEquals(new ItemUpdate(subject), update);
    }
    
    @Test
    public void testGetters() {
        WbMonolingualExpr monolingualExpr = TestingDataGenerator.getTestMonolingualExpr("en", "English", "not sure what");
        WbNameDescExpr aliasExpr = new WbNameDescExpr(WbNameDescExpr.NameDescrType.ALIAS,
                monolingualExpr);
        assertEquals(WbNameDescExpr.NameDescrType.ALIAS, aliasExpr.getType());
        assertEquals(monolingualExpr, aliasExpr.getValue());
    }
    
    @Test
    public void testSerialization() {
        JacksonSerializationTest.canonicalSerialization(WbNameDescExpr.class, expr, jsonRepresentation);
    }
}
