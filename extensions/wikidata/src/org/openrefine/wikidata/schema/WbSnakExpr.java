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

import org.jsoup.helper.Validate;
import org.openrefine.wikidata.schema.entityvalues.FullyPropertySerializingValueSnak;
import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * An expression for a snak (pair of property and value).
 * 
 * @author Antonin Delpeuch
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public class WbSnakExpr implements WbExpression<Snak> {

    private WbExpression<? extends PropertyIdValue> prop;
    private WbExpression<? extends Value> value;

    @JsonCreator
    public WbSnakExpr(@JsonProperty("prop") WbExpression<? extends PropertyIdValue> propExpr,
            @JsonProperty("value") WbExpression<? extends Value> valueExpr) {
        Validate.notNull(propExpr);
        this.prop = propExpr;
        Validate.notNull(valueExpr);
        this.value = valueExpr;
    }

    @Override
    public Snak evaluate(ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        PropertyIdValue propertyId = getProp().evaluate(ctxt);
        Value evaluatedValue = value.evaluate(ctxt);
        return new FullyPropertySerializingValueSnak(propertyId, evaluatedValue);
    }

    @JsonProperty("prop")
    public WbExpression<? extends PropertyIdValue> getProp() {
        return prop;
    }

    @JsonProperty("value")
    public WbExpression<? extends Value> getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !WbSnakExpr.class.isInstance(other)) {
            return false;
        }
        WbSnakExpr otherExpr = (WbSnakExpr) other;
        return prop.equals(otherExpr.getProp()) && value.equals(otherExpr.getValue());
    }

    @Override
    public int hashCode() {
        return prop.hashCode() + value.hashCode();
    }
}
