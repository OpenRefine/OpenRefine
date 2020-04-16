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
import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WbMonolingualExpr implements WbExpression<MonolingualTextValue> {

    private WbExpression<? extends String> languageExpr;
    private WbExpression<? extends StringValue> valueExpr;

    @JsonCreator
    public WbMonolingualExpr(@JsonProperty("language") WbExpression<? extends String> languageExpr,
            @JsonProperty("value") WbExpression<? extends StringValue> valueExpr) {
        Validate.notNull(languageExpr);
        this.languageExpr = languageExpr;
        Validate.notNull(valueExpr);
        this.valueExpr = valueExpr;
    }

    @Override
    public MonolingualTextValue evaluate(ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        String text = getValueExpr().evaluate(ctxt).getString();
        try {
            String lang = getLanguageExpr().evaluate(ctxt);
            return Datamodel.makeMonolingualTextValue(text.trim(), lang);

        } catch (SkipSchemaExpressionException e) {
            QAWarning warning = new QAWarning("monolingual-text-without-language", null, QAWarning.Severity.WARNING, 1);
            warning.setProperty("example_text", text);
            ctxt.addWarning(warning);
            throw new SkipSchemaExpressionException();
        }
    }

    @JsonProperty("language")
    public WbExpression<? extends String> getLanguageExpr() {
        return languageExpr;
    }

    @JsonProperty("value")
    public WbExpression<? extends StringValue> getValueExpr() {
        return valueExpr;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !WbMonolingualExpr.class.isInstance(other)) {
            return false;
        }
        WbMonolingualExpr otherExpr = (WbMonolingualExpr) other;
        return languageExpr.equals(otherExpr.getLanguageExpr()) && valueExpr.equals(otherExpr.getValueExpr());
    }

    @Override
    public int hashCode() {
        return languageExpr.hashCode() + valueExpr.hashCode();
    }
}
