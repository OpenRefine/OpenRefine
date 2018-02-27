package org.openrefine.wikidata.schema;

import java.math.BigDecimal;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;

public class WbQuantityExprTest extends WbExpressionTest<QuantityValue> {
    
    private WbQuantityExpr exprWithUnit = new WbQuantityExpr(new WbStringVariable("column A"),
            new WbItemVariable("column B"));
    private WbQuantityExpr exprWithoutUnit = new WbQuantityExpr(new WbStringVariable("column A"),
            null);
    
    @Test
    public void testWithoutUnit() throws SkipSchemaExpressionException {
        setRow("4.00");
        evaluatesTo(Datamodel.makeQuantityValue(new BigDecimal("4.00"), null, null, "1"), exprWithoutUnit);
    }
    
    public void testInvalidAmountWithoutUnit() {
        setRow("hello");
        isSkipped(exprWithoutUnit);
    }
    
    @Test
    public void testWithUnit() throws SkipSchemaExpressionException {
        setRow("56.094", recon("Q42"));
        evaluatesTo(Datamodel.makeQuantityValue(new BigDecimal("56.094"), null, null, "http://www.wikidata.org/entity/Q42"), exprWithUnit);
    }
    
    public void testInvalidAmountWithUnit() throws SkipSchemaExpressionException {
        setRow("invalid", recon("Q42"));
        isSkipped(exprWithUnit);
    }
    
    public void testInvalidUnitWithAmount() throws SkipSchemaExpressionException {
        setRow("56.094", "not reconciled");
        isSkipped(exprWithUnit);
    }

}
