package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WbLanguageConstant extends WbLanguageExpr {
    
    protected String _langId;
    protected String _langLabel;
    
    @JsonCreator
    public WbLanguageConstant(
            @JsonProperty("id") String langId,
            @JsonProperty("label") String langLabel) {
        _langId = langId;
        _langLabel = langLabel;
    }
    
    public String evaluate(ExpressionContext ctxt) throws SkipSchemaExpressionException {
        return _langId;
    }
    
    @JsonProperty("id")
    public String getLang() {
        return _langId;
    }
    
    @JsonProperty("label")
    public String getLabel() {
        return _langLabel;
    }
    
}
