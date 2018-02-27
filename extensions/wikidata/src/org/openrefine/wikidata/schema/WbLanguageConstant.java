package org.openrefine.wikidata.schema;

import org.apache.commons.lang.Validate;
import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
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
    public WbLanguageConstant(
            @JsonProperty("id") String langId,
            @JsonProperty("label") String langLabel) {
        _langId = normalizeLanguageCode(langId);
        Validate.notNull(_langId, "A valid language code must be provided.");
        Validate.notNull(langLabel);
        _langLabel = langLabel;
    }
    
    /**
     * Checks that a language code is valid and returns its preferred
     * version (converting deprecated language codes to their better values).
     * 
     * @param lang
     *      a Wikimedia language code
     * @return
     *      the normalized code, or null if the code is invalid.
     */
    public static String normalizeLanguageCode(String lang) {
        try {
            WikimediaLanguageCodes.getLanguageCode(lang);
            return WikimediaLanguageCodes.fixLanguageCodeIfDeprecated(lang);
        } catch(IllegalArgumentException e) {
            return null;
        }
    }
    
    @Override
    public String evaluate(ExpressionContext ctxt) throws SkipSchemaExpressionException {
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
        if(other == null || !WbLanguageConstant.class.isInstance(other)) {
            return false;
        }
        WbLanguageConstant otherConstant = (WbLanguageConstant)other;
        return _langId.equals(otherConstant.getLang()) && _langLabel.equals(otherConstant.getLabel());
    }
    
}
