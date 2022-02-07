
package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;

public class WbEntityIdValueConstantTest extends WbExpressionTest<EntityIdValue> {

    private WbEntityIdValueConstant constant = new WbEntityIdValueConstant("P48", "my ID");

    @Test
    public void testEvaluate() {
        EntityIdValue result = constant.evaluate(ctxt);
        Assert.assertEquals(Datamodel.makeWikidataPropertyIdValue("P48").getIri(), result.getIri());
    }

    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, constant,
                "{\"type\":\"wbentityidvalueconstant\",\"id\":\"P48\",\"label\":\"my ID\"}");
    }
}
