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

import org.openrefine.wikibase.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.openrefine.wikibase.utils.LanguageCodeStore;
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
    protected String _origLangId; // for error reporting purposes during validation
    protected String _langLabel;

    @JsonCreator
    public WbLanguageConstant(@JsonProperty("id") String langId, @JsonProperty("label") String langLabel) {
        _langId = normalizeLanguageCode(langId);
        _langLabel = langLabel;
        _origLangId = langId;
    }

    @Override
    public void validate(ValidationState validation) {
        if (_origLangId != null && _langId == null) {
            validation.addError("Invalid language code '" + _origLangId + "'");
        } else if (_langId == null) {
            validation.addError("Empty language field");
        }
        if (_langLabel == null) {
            validation.addError("Empty text field");
        }
    }

    public static String normalizeLanguageCode(String lang) {
        return normalizeLanguageCode(lang, null);
    }

    /**
     * Checks that a language code is valid and returns its preferred version (converting deprecated language codes to
     * their better values).
     * 
     * @param lang
     *            a Wikimedia language code
     * @param mediaWikiApiEndpoint
     *            the MediaWiki API endpoint of the Wikibase
     * @return the normalized code, or null if the code is invalid.
     */
    public static String normalizeLanguageCode(String lang, String mediaWikiApiEndpoint) {
        try {
            if (LanguageCodeStore.getLanguageCodes(mediaWikiApiEndpoint).contains(lang)) {
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
