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
package org.openrefine.wikidata.schema;

import org.apache.commons.lang.Validate;
import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikidata.utils.LanguageCodeStore;
import org.wikidata.wdtk.datamodel.interfaces.WikimediaLanguageCodes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A constant that represents a Wikimedia language code.
 * 
 * @author Antonin Delpeuch
 *
 */
public class WbLanguageConstant implements WbExpression<String> {

    protected String _langId;
    protected String _langLabel;

    @JsonCreator
    public WbLanguageConstant(@JsonProperty("id") String langId, @JsonProperty("label") String langLabel) {
        _langId = normalizeLanguageCode(langId);
        Validate.notNull(_langId, "A valid language code must be provided.");
        Validate.notNull(langLabel);
        _langLabel = langLabel;
    }

    /**
     * Checks that a language code is valid and returns its preferred version
     * (converting deprecated language codes to their better values).
     * 
     * @param lang
     *            a Wikimedia language code
     * @return the normalized code, or null if the code is invalid.
     */
    public static String normalizeLanguageCode(String lang) {
        try {
        	if (LanguageCodeStore.ALLOWED_LANGUAGE_CODES.contains(lang)) {
        		return WikimediaLanguageCodes.fixLanguageCodeIfDeprecated(lang);
        	} else {
        		return null;
        	}
        } catch (IllegalArgumentException e) {
        	return null;
        }
    }

    @Override
    public String evaluate(ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        return _langId;
    }

    /**
     * @return the language code for this language
     */
    @JsonProperty("id")
    public String getLang() {
        return _langId;
    }

    /**
     * @return the name of the language in itself
     */
    @JsonProperty("label")
    public String getLabel() {
        return _langLabel;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !WbLanguageConstant.class.isInstance(other)) {
            return false;
        }
        WbLanguageConstant otherConstant = (WbLanguageConstant) other;
        return _langId.equals(otherConstant.getLang()) && _langLabel.equals(otherConstant.getLabel());
    }

    @Override
    public int hashCode() {
        return _langId.hashCode();
    }

}
