package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public class WbItemConstantTest extends WbExpressionTest<ItemIdValue> {
    
    private WbItemConstant constant = new WbItemConstant("Q42", "Douglas Adams");
    
    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, constant,
                "{\"type\":\"wbitemconstant\",\"qid\":\"Q42\",\"label\":\"Douglas Adams\"}");
    }
    
    @Test
    public void testEvaluate() {
        evaluatesTo(Datamodel.makeWikidataItemIdValue("Q42"), constant);
    }
}
