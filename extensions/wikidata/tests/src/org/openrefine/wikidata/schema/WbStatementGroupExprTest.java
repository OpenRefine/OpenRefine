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
        expr = new WbStatementGroupExpr(propertyExpr, Collections.singletonList(statementTest.statementExpr));
        subject = statementTest.subject;
        statementGroup = Datamodel.makeStatementGroup(Collections.singletonList(statementTest.fullStatement));
        jsonRepresentation = "{\"property\":{\"type\":\"wbpropconstant\",\"pid\":\"P908\",\"label\":\"myprop\",\"datatype\":\"time\"},"
                + "\"statements\":[" + statementTest.jsonRepresentation + "]}";
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
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
    public void testSerialize()
            throws JsonProcessingException {
        JacksonSerializationTest.canonicalSerialization(WbStatementGroupExpr.class, expr, jsonRepresentation);
    }
}
