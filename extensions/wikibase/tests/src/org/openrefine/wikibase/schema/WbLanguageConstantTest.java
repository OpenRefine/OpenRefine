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
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.google.refine.model.Column;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.ModelException;

import org.openrefine.wikibase.schema.validation.ValidationState;
import org.openrefine.wikibase.testing.JacksonSerializationTest;
import org.openrefine.wikibase.utils.LanguageCodeStore.LanguageCodeContext;

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

    @Test
    public void testValidationWithTermContextRejectsMonolingualOnlyCode() throws ModelException {
        // "und" is valid for monolingualtext but not for term (labels/descriptions/aliases)
        ColumnModel columnModel = new ColumnModel();
        columnModel.addColumn(0, new Column(0, "column"), true);
        ValidationState validation = new ValidationState(columnModel);
        validation.setMediaWikiApiEndpoint(null);
        validation.setLanguageContext(LanguageCodeContext.TERM);
        WbLanguageConstant undConstant = new WbLanguageConstant("und", "Undefined");
        undConstant.validate(validation);
        assertTrue(validation.getValidationErrors().stream().anyMatch(e -> e.getMessage().contains("und")),
                "Term context should reject language code 'und'");
    }

    @Test
    public void testNormalizeLanguageCodeWithContext() {
        assertEquals("de", WbLanguageConstant.normalizeLanguageCode("de", null, LanguageCodeContext.TERM));
        assertEquals("de", WbLanguageConstant.normalizeLanguageCode("de", null, LanguageCodeContext.MONOLINGUALTEXT));
        assertNull(WbLanguageConstant.normalizeLanguageCode("und", null, LanguageCodeContext.TERM));
        assertEquals("und", WbLanguageConstant.normalizeLanguageCode("und", null, LanguageCodeContext.MONOLINGUALTEXT));
    }
}
