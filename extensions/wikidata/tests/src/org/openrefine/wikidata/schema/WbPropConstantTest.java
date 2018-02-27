package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

public class WbPropConstantTest extends WbExpressionTest<PropertyIdValue> {
    
    private WbPropConstant constant = new WbPropConstant("P48", "my ID", "external-id");
    
    @Test
    public void testEvaluate() {
        evaluatesTo(Datamodel.makeWikidataPropertyIdValue("P48"), constant);
    }
    
    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, constant,
                "{\"type\":\"wbpropconstant\",\"pid\":\"P48\",\"label\":\"my ID\",\"datatype\":\"external-id\"}");
    }
}
