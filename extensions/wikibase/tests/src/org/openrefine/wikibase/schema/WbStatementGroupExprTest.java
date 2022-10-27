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

import java.util.Collections;

import org.openrefine.wikibase.schema.exceptions.QAWarningException;
import org.openrefine.wikibase.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.openrefine.wikibase.testing.JacksonSerializationTest;
import org.openrefine.wikibase.updates.StatementGroupEdit;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.refine.model.Column;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.ModelException;

public class WbStatementGroupExprTest extends WbExpressionTest<StatementGroupEdit> {

    private WbPropConstant propertyExpr = new WbPropConstant("P908", "myprop", "time");
    public WbStatementGroupExpr expr;
    public WbStatementGroupExpr noPropertyExpr;

    private EntityIdValue subject;
    public StatementGroupEdit statementGroupUpdate;

    public String jsonRepresentation;

    class Wrapper implements WbExpression<StatementGroupEdit> {

        public WbStatementGroupExpr expr;

        public Wrapper(WbStatementGroupExpr e) {
            expr = e;
        }

        @Override
        public StatementGroupEdit evaluate(ExpressionContext ctxt)
                throws SkipSchemaExpressionException, QAWarningException {
            return expr.evaluate(ctxt, subject);
        }

        @Override
        public void validate(ValidationState validation) {
            expr.validate(validation);
        }
    }

    public WbStatementGroupExprTest() {
        WbStatementExprTest statementTest = new WbStatementExprTest();
        expr = new WbStatementGroupExpr(propertyExpr, Collections.singletonList(statementTest.statementExpr));
        noPropertyExpr = new WbStatementGroupExpr(null, Collections.singletonList(statementTest.statementExpr));
        subject = statementTest.subject;
        statementGroupUpdate = new StatementGroupEdit(Collections.singletonList(statementTest.fullStatementUpdate));
        jsonRepresentation = "{\"property\":{\"type\":\"wbpropconstant\",\"pid\":\"P908\",\"label\":\"myprop\",\"datatype\":\"time\"},"
                + "\"statements\":[" + statementTest.jsonRepresentation + "]}";
    }

    @Test
    public void testEmptyStatements() {
        WbStatementGroupExpr emptyStatements = new WbStatementGroupExpr(propertyExpr, Collections.emptyList());
        hasValidationError("No statements", new Wrapper(emptyStatements), new ColumnModel());
    }

    @Test
    public void testEvaluate() {
        setRow(recon("Q3434"), "2010-07-23", "3.898,4.389");
        evaluatesTo(statementGroupUpdate, new Wrapper(expr));
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

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testUnmodifiableList() {
        expr.getStatements().clear();
    }

    @Test
    public void testValidate() throws ModelException {
        ColumnModel columnModel = new ColumnModel();
        columnModel.addColumn(0, new Column(0, "column A"), true);
        columnModel.addColumn(1, new Column(1, "column B"), true);
        columnModel.addColumn(2, new Column(2, "column C"), true);

        hasNoValidationError(new Wrapper(expr), columnModel);
        hasValidationError("No property", new Wrapper(noPropertyExpr), columnModel);
    }
}
