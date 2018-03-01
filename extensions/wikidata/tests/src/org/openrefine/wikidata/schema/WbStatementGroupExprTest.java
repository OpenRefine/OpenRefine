package org.openrefine.wikidata.schema;

import java.util.Collections;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;

import com.fasterxml.jackson.core.JsonProcessingException;

public class WbStatementGroupExprTest extends WbExpressionTest<StatementGroup> {
    
    private WbPropConstant propertyExpr = new WbPropConstant("P908", "myprop", "time");
    public WbStatementGroupExpr expr;
    
    private ItemIdValue subject;
    public StatementGroup statementGroup;
    
    public String jsonRepresentation;
    
    
    class Wrapper implements WbExpression<StatementGroup> {
        public WbStatementGroupExpr expr;
        
        public Wrapper(WbStatementGroupExpr e) {
            expr = e;
        }

        @Override
        public StatementGroup evaluate(ExpressionContext ctxt)
                throws SkipSchemaExpressionException {
            return expr.evaluate(ctxt, subject);
        }
    }
    
    
    public WbStatementGroupExprTest() {  
        WbStatementExprTest statementTest = new WbStatementExprTest();
        expr = new WbStatementGroupExpr(
                propertyExpr,
                Collections.singletonList(statementTest.statementExpr));
        subject = statementTest.subject;
        statementGroup = Datamodel.makeStatementGroup(Collections.singletonList(statementTest.fullStatement));
        jsonRepresentation = "{\"property\":{\"type\":\"wbpropconstant\",\"pid\":\"P908\",\"label\":\"myprop\",\"datatype\":\"time\"},"
                +"\"statements\":["+statementTest.jsonRepresentation+"]}";
    }
    
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testCreate() {
        new WbStatementGroupExpr(propertyExpr, Collections.emptyList());
    }
    
    @Test
    public void testEvaluate() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,4.389");
        evaluatesTo(statementGroup, new Wrapper(expr));
    }
    
    @Test
    public void testSkip() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,invalid");
        isSkipped(new Wrapper(expr));
    }
    
    @Test
    public void testSerialize() throws JsonProcessingException {
        JacksonSerializationTest.canonicalSerialization(WbStatementGroupExpr.class, expr, jsonRepresentation);
    }
}
