package org.openrefine.wikidata.schema;

import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class WbMonolingualExpr extends WbValueExpr {
    
    private WbLanguageExpr languageExpr;
    private WbStringExpr valueExpr;
    
    @JsonCreator
    public WbMonolingualExpr(
            @JsonProperty("language") WbLanguageExpr languageExpr,
            @JsonProperty("value") WbStringExpr valueExpr) {
        this.languageExpr = languageExpr;
        this.valueExpr = valueExpr;
    }

    @Override
    public MonolingualTextValue evaluate(ExpressionContext ctxt)
            throws SkipStatementException {
        return Datamodel.makeMonolingualTextValue(
                getValueExpr().evaluate(ctxt).getString(),
                getLanguageExpr().evaluate(ctxt));
    }

    public WbLanguageExpr getLanguageExpr() {
        return languageExpr;
    }

    public WbStringExpr getValueExpr() {
        return valueExpr;
    }
}
