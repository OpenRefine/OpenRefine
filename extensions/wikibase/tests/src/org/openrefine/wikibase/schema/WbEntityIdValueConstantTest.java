
package org.openrefine.wikibase.schema;

import org.openrefine.wikibase.testing.JacksonSerializationTest;
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

    @Test
    public void testValidate() {
        hasNoValidationError(constant);
        hasValidationError("No entity label provided", new WbEntityIdValueConstant("P48", null));
        hasValidationError("No entity id provided", new WbEntityIdValueConstant(null, "my label"));
        hasValidationError("Invalid entity id format: 'invalid format'", new WbEntityIdValueConstant("invalid format", "my label"));

    }
}
