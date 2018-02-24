package org.openrefine.wikidata.schema;

import java.io.IOException;
import java.math.BigDecimal;

import org.openrefine.wikidata.qa.QAWarningStore;
import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;

import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.tests.RefineTest;

public class WbQuantityValueTest extends RefineTest {

    protected Project project;
    protected Row row;
    protected ExpressionContext ctxt;
    protected QAWarningStore warningStore;
    
    @BeforeMethod
    public void createProject() throws IOException, ModelException {
        project = createCSVProject("Wikidata quantity value test project", 
                "column A\nrow1");
        warningStore = new QAWarningStore();
        row = project.rows.get(0);
        ctxt = new ExpressionContext("http://www.wikidata.org/entity/", 0,
                row, project.columnModel, warningStore);
    }
    
    @Test
    public void testWithoutUnit() throws SkipSchemaExpressionException {
        WbQuantityExpr expr = new WbQuantityExpr(new WbStringConstant("4.00"), null);
        Assert.assertEquals(Datamodel.makeQuantityValue(new BigDecimal("4.00"), null, null, "1"), expr.evaluate(ctxt));
    }
    
    @Test(expectedExceptions = SkipSchemaExpressionException.class)
    public void testInvalidAmountWithoutUnit() throws SkipSchemaExpressionException {
        WbQuantityExpr expr = new WbQuantityExpr(new WbStringConstant("hello"), null);
        expr.evaluate(ctxt);
    }
    
    @Test
    public void testWithUnit() throws SkipSchemaExpressionException {
        WbItemConstant item = new WbItemConstant("Q42", "label");
        WbQuantityExpr expr = new WbQuantityExpr(new WbStringConstant("56.094"), item);
        Assert.assertEquals(Datamodel.makeQuantityValue(new BigDecimal("56.094"), null, null, "http://www.wikidata.org/entity/Q42"), expr.evaluate(ctxt));
    }
    
    @Test(expectedExceptions = SkipSchemaExpressionException.class)
    public void testInvalidAmountWithUnit() throws SkipSchemaExpressionException {
        WbItemConstant item = new WbItemConstant("Q42", "label");
        WbQuantityExpr expr = new WbQuantityExpr(new WbStringConstant("invalid"), item);
        expr.evaluate(ctxt);
    }
    
    @Test(expectedExceptions = SkipSchemaExpressionException.class)
    public void testInvalidUnitWithAmount() throws SkipSchemaExpressionException {
        WbItemVariable item = new WbItemVariable();
        item.setColumnName("column A");
        WbQuantityExpr expr = new WbQuantityExpr(new WbStringConstant("56.094"), item);
        expr.evaluate(ctxt);
    }
}
