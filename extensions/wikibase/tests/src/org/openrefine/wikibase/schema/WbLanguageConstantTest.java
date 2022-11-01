/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package org.openrefine.wikibase.schema;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.openrefine.wikibase.testing.JacksonSerializationTest;
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
        assertEquals("mul", WbLanguageConstant.normalizeLanguageCode("mul"));
        assertNull(WbLanguageConstant.normalizeLanguageCode("non-existent language code"));
        assertNull(WbLanguageConstant.normalizeLanguageCode(null));
    }

    @Test
    public void testFallbackLangCodes() {
        assertEquals("de", WbLanguageConstant.normalizeLanguageCode("de", "http://not.exist/w/api.php"));
    }

    @Test
    public void testValidate() {
        hasNoValidationError(constant);
        hasValidationError("Empty language field", new WbLanguageConstant(null, "Deutsch"));
        hasValidationError("Empty text field", new WbLanguageConstant("de", null));
        hasValidationError("Invalid language code 'invalid_code'", new WbLanguageConstant("invalid_code", "unknown language"));
    }
}
