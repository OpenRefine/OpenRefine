package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.annotations.Test;

public class WbLanguageVariableTest extends WbVariableTest<String> {

    @Override
    public WbVariableExpr<String> initVariableExpr() {
        return new WbLanguageVariable();
    }
    
    @Test
    public void testValidLanguageCode() {
        evaluatesTo("en", "en");
        evaluatesTo("nb", "no");
        evaluatesTo("de", "   de ");
    }
    
    @Test
    public void testInvalidLanguageCode() {
        isSkipped("unknown language code");
        isSkipped((String)null);
        isSkipped("");
    }

    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, variable,
                "{\"type\":\"wblanguagevariable\",\"columnName\":\"column A\"}");
    }
}
