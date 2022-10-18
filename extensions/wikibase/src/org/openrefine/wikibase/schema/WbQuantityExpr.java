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

import java.math.BigDecimal;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.schema.exceptions.QAWarningException;
import org.openrefine.wikibase.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikibase.schema.validation.PathElement;
import org.openrefine.wikibase.schema.validation.PathElement.Type;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WbQuantityExpr implements WbExpression<QuantityValue> {

    private final WbExpression<? extends StringValue> amountExpr;
    private final WbExpression<? extends ItemIdValue> unitExpr;

    /**
     * Creates an expression for a quantity, which contains two sub-expressions: one for the amount (a string with a
     * particular format) and one for the unit, which is optional.
     * 
     * Setting unitExpr to null will give quantities without units. Setting it to a non-null value will make the unit
     * mandatory: if the unit expression fails to evaluate, the whole quantity expression will fail too.
     */
    @JsonCreator
    public WbQuantityExpr(@JsonProperty("amount") WbExpression<? extends StringValue> amountExpr,
            @JsonProperty("unit") WbExpression<? extends ItemIdValue> unitExpr) {
        this.amountExpr = amountExpr;
        this.unitExpr = unitExpr;
    }

    @Override
    public void validate(ValidationState validation) {
        if (amountExpr == null) {
            validation.addError("No quantity amount provided");
        } else {
            amountExpr.validate(validation);
        }
        if (unitExpr != null) {
            validation.enter(new PathElement(Type.UNIT));
            unitExpr.validate(validation);
            validation.leave();
        }
    }

    @Override
    public QuantityValue evaluate(ExpressionContext ctxt)
            throws SkipSchemaExpressionException, QAWarningException {
        StringValue amount = getAmountExpr().evaluate(ctxt);
        // we know the amount is nonnull, nonempty here

        BigDecimal parsedAmount = null;
        BigDecimal lowerBound = null;
        BigDecimal upperBound = null;
        String originalAmount = amount.getString().toUpperCase();
        try {
            parsedAmount = new BigDecimal(originalAmount);

            if (originalAmount.contains("E")) {
                // engineering notation: we derive the precision from
                // the expression (feature!)
                BigDecimal uncertainty = new BigDecimal("0.5").scaleByPowerOfTen(-parsedAmount.scale());
                lowerBound = new BigDecimal(parsedAmount.subtract(uncertainty).toPlainString());
                upperBound = new BigDecimal(parsedAmount.add(uncertainty).toPlainString());
            }
            // workaround for https://github.com/Wikidata/Wikidata-Toolkit/issues/341
            parsedAmount = new BigDecimal(parsedAmount.toPlainString());
        } catch (NumberFormatException e) {
            if (!originalAmount.isEmpty()) {
                QAWarning issue = new QAWarning("ignored-amount", null, QAWarning.Severity.WARNING, 1);
                issue.setProperty("example_value", originalAmount);
                ctxt.addWarning(issue);
            }
            throw new SkipSchemaExpressionException();
        }

        if (getUnitExpr() != null) {
            ItemIdValue unit = getUnitExpr().evaluate(ctxt);
            return Datamodel.makeQuantityValue(parsedAmount, lowerBound, upperBound, unit);
        }

        return Datamodel.makeQuantityValue(parsedAmount, lowerBound, upperBound);
    }

    @JsonProperty("amount")
    public WbExpression<? extends StringValue> getAmountExpr() {
        return amountExpr;
    }

    @JsonProperty("unit")
    public WbExpression<? extends ItemIdValue> getUnitExpr() {
        return unitExpr;
    }

}
