package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class WbMonolingualExpr implements WbValueExpr<MonolingualTextValue> {
    
    private WbValueExpr<? extends String> languageExpr;
    private WbValueExpr<? extends StringValue> valueExpr;
    
    @JsonCreator
    public WbMonolingualExpr(
            @JsonProperty("language") WbValueExpr<? extends String> languageExpr,
            @JsonProperty("value") WbValueExpr<? extends StringValue> valueExpr) {
        this.languageExpr = languageExpr;
        this.valueExpr = valueExpr;
    }

    @Override
    public MonolingualTextValue evaluate(ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        String text = getValueExpr().evaluate(ctxt).getString();
        if (text.isEmpty())
            throw new SkipSchemaExpressionException();
        String lang = getLanguageExpr().evaluate(ctxt);
        if (lang.isEmpty()) {
            QAWarning warning = new QAWarning(
                  "monolingual-text-without-language",
                  null,
                  QAWarning.Severity.WARNING,
                  1);
            warning.setProperty("example_text", text);
            ctxt.addWarning(warning);
            throw new SkipSchemaExpressionException();
        }
        
        return Datamodel.makeMonolingualTextValue(text, lang);
        
    }

    @JsonProperty("language")
    public WbValueExpr<? extends String> getLanguageExpr() {
        return languageExpr;
    }

    @JsonProperty("value")
    public WbValueExpr<? extends StringValue> getValueExpr() {
        return valueExpr;
    }
}
