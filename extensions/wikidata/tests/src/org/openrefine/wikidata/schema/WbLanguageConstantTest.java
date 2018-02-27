package org.openrefine.wikidata.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.annotations.Test;

public class WbLanguageConstantTest extends WbExpressionTest<String> {
    
    private WbLanguageConstant constant = new WbLanguageConstant("de", "Deutsch");
    
    @Test
    public void testEvaluation() {
        evaluatesTo("de", constant);
    }
    
    @Test
    public void testSerialization() {
        JacksonSerializationTest.canonicalSerialization(WbExpression.class, constant,
                "{\"type\":\"wblanguageconstant\",\"id\":\"de\",\"label\":\"Deutsch\"}");
    }
    
    @Test
    public void testNormalizeLanguageCode() {
        assertEquals("ku-latn", WbLanguageConstant.normalizeLanguageCode("ku-latn"));
        assertEquals("de", WbLanguageConstant.normalizeLanguageCode("de"));
        assertEquals("nb", WbLanguageConstant.normalizeLanguageCode("no"));
        assertEquals("nb", WbLanguageConstant.normalizeLanguageCode("nb"));
        assertNull(WbLanguageConstant.normalizeLanguageCode("non-existent language code"));
        assertNull(WbLanguageConstant.normalizeLanguageCode(null));
    }
}
