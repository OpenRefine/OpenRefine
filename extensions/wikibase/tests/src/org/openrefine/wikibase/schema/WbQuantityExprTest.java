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

import java.math.BigDecimal;

import org.openrefine.wikibase.schema.exceptions.QAWarningException;
import org.openrefine.wikibase.schema.exceptions.SkipSchemaExpressionException;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.refine.model.Column;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.ModelException;

public class WbQuantityExprTest extends WbExpressionTest<QuantityValue> {

    private WbQuantityExpr exprWithUnit = new WbQuantityExpr(new WbStringVariable("column A"),
            new WbItemVariable("column B"));
    private WbQuantityExpr exprWithoutUnit = new WbQuantityExpr(new WbStringVariable("column A"), null);

    @Test
    public void testWithoutUnit()
            throws SkipSchemaExpressionException {
        setRow("4.00");
        evaluatesTo(Datamodel.makeQuantityValue(new BigDecimal("4.00")), exprWithoutUnit);
    }

    @Test
    public void testOverflow() {
        setRow(14341937500d);
        evaluatesTo(Datamodel.makeQuantityValue(new BigDecimal("14341937500")), exprWithoutUnit);
    }

    @Test
    public void testInvalidAmountWithoutUnit() {
        setRow("hello");
        isSkipped(exprWithoutUnit);
    }

    @Test
    public void testWithUnit()
            throws SkipSchemaExpressionException {
        setRow("56.094", recon("Q42"));
        evaluatesTo(
                Datamodel.makeQuantityValue(new BigDecimal("56.094"), Datamodel.makeWikidataItemIdValue("Q42")),
                exprWithUnit);
    }

    @Test
    public void testInvalidAmountWithUnit()
            throws SkipSchemaExpressionException {
        setRow("invalid", recon("Q42"));
        isSkipped(exprWithUnit);
    }

    @Test
    public void testInvalidUnitWithAmount()
            throws SkipSchemaExpressionException {
        setRow("56.094", "not reconciled");
        isSkipped(exprWithUnit);
    }

    // for issue #341: https://github.com/Wikidata/Wikidata-Toolkit/issues/341
    @Test
    public void testExponent() throws SkipSchemaExpressionException, JsonProcessingException, QAWarningException {
        setRow("38.4E+3", recon("Q42"));
        QuantityValue val = exprWithUnit.evaluate(ctxt);
        assertEquals("38400", val.getNumericValue().toString());
        assertEquals("38350", val.getLowerBound().toString());
        assertEquals("38450", val.getUpperBound().toString());
    }

    @Test
    public void testValidate() throws ModelException {
        ColumnModel columnModel = new ColumnModel();
        columnModel.addColumn(0, new Column(0, "column A"), true);
        columnModel.addColumn(0, new Column(0, "column B"), true);

        hasNoValidationError(exprWithUnit, columnModel);
        hasNoValidationError(exprWithoutUnit, columnModel);
        hasValidationError("No quantity amount provided", new WbQuantityExpr(null,
                new WbItemVariable("column B")), columnModel);
    }

}
