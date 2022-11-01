
package org.openrefine.wikibase.schema;

import org.openrefine.wikibase.testing.JacksonSerializationTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

public class WbStringConstantTest extends WbExpressionTest<StringValue> {

    private WbStringConstant constant = new WbStringConstant("hello world");

    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, constant,
                "{\"type\":\"wbstringconstant\",\"value\":\"hello world\"}");
    }

    @Test
    public void testEvaluate() {
        evaluatesTo(Datamodel.makeStringValue("hello world"), constant);
    }

    @Test
    public void testTrim() {
        evaluatesTo(Datamodel.makeStringValue("hello world"), new WbStringConstant(" hello world "));
    }

    @Test
    public void testEmpty() {
        hasValidationError("Empty value", new WbStringConstant(""));
    }

    @Test
    public void testValidate() {
        hasNoValidationError(constant);
    }
}
