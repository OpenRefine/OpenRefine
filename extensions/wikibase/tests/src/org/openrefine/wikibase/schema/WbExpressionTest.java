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

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.qa.QAWarningStore;
import org.openrefine.wikibase.schema.exceptions.QAWarningException;
import org.openrefine.wikibase.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.openrefine.wikibase.testing.TestingData;
import org.openrefine.wikibase.testing.WikidataRefineTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Row;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class WbExpressionTest<T> extends WikidataRefineTest {

    protected Project project;
    protected Row row;
    protected ExpressionContext ctxt;
    protected QAWarningStore warningStore;

    protected static MockWebServer server;

    @BeforeClass
    public void startServer() throws IOException {
        server = new MockWebServer();
        String json = TestingData.jsonFromFile("langcode/wikidata-monolingualtext-langcode.json");
        server.setDispatcher(new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return new MockResponse()
                        .addHeader("Content-Type", "application/json; charset=utf-8")
                        .setBody(json);
            }
        });
        server.start();
    }

    @AfterClass
    public void shutdownServer() throws IOException {
        server.shutdown();
    }

    @BeforeMethod
    public void createProject()
            throws IOException, ModelException {
        project = createCSVProject("Wikidata variable test project",
                "column A,column B,column C,column D,column E\n" + "value A,value B,value C,value D,value E");
        warningStore = new QAWarningStore();
        row = project.rows.get(0);
        ctxt = new ExpressionContext("http://www.wikidata.org/entity/", Collections.emptyMap(), server.url("/w/api.php").toString(), 0, row,
                project.columnModel, warningStore);
    }

    /**
     * Test that a particular expression evaluates to some object.
     * 
     * @param expected
     *            the expected evaluation of the value
     * @param expression
     *            the expression to evaluate
     */
    public void evaluatesTo(T expected, WbExpression<T> expression) {
        ValidationState validation = new ValidationState(ctxt.getColumnModel());
        expression.validate(validation);

        try {
            T result = expression.evaluate(ctxt);
            Assert.assertEquals(expected, result);
        } catch (SkipSchemaExpressionException e) {
            Assert.fail("Value was skipped by evaluator");
        } catch (QAWarningException e) {
            Assert.fail("The evaluator threw a QA warning instead");
        }
    }

    /**
     * Test that a particular expression is skipped.
     * 
     * @param expression
     *            the expression to evaluate
     */
    public void isSkipped(WbExpression<T> expression) {
        try {
            expression.evaluate(ctxt);
            Assert.fail("Value was not skipped by evaluator");
        } catch (SkipSchemaExpressionException e) {
            return;
        } catch (QAWarningException e) {
            Assert.fail("The evaluator threw a QA warning instead");
        }
    }

    /**
     * Test that a particular expression raises a QA warning at evaluation time (therefore not yielding any result).
     */
    public void evaluatesToWarning(QAWarning warning, WbExpression<T> expression) {
        try {
            expression.evaluate(ctxt);
            Assert.fail("The evaluator returned a value, not a warning");
        } catch (SkipSchemaExpressionException e) {
            Assert.fail("The value was skipped by the evaluator");
        } catch (QAWarningException e) {
            Assert.assertEquals(e.getWarning(), warning);
        }
    }

    /**
     * Tests that an expression has a validation error. Used when the column model is irrelevant to the validation
     * problem.
     */
    public void hasValidationError(String errorMessage, WbExpression<T> expression) {
        ColumnModel columnModel = new ColumnModel();
        try {
            columnModel.addColumn(0, new Column(0, "column"), true);
        } catch (ModelException e) {
        }
        hasValidationError(errorMessage, expression, columnModel);
    }

    /**
     * Tests that an expression has a validation error. Used when the column model is relevant to the validation
     * problem.
     */
    public void hasValidationError(String errorMessage, WbExpression<T> expression, ColumnModel columnModel) {
        ValidationState validationState = new ValidationState(columnModel);
        expression.validate(validationState);
        List<String> validationMessages = validationState.getValidationErrors()
                .stream()
                .map(e -> e.getMessage())
                .collect(Collectors.toList());
        if (!Collections.singletonList(errorMessage).equals(validationMessages)) {
            Assert.fail("Unexpected validation status: expected error '"
                    + errorMessage + "', but found errors: " + validationMessages.toString());
        }
    }

    /**
     * Tests that an expression has no validation error.
     */
    public void hasNoValidationError(WbExpression<T> expression) {
        ColumnModel columnModel = new ColumnModel();
        try {
            columnModel.addColumn(0, new Column(0, "column"), true);
        } catch (ModelException e) {
        }
        hasNoValidationError(expression, columnModel);
    }

    /**
     * Tests that an expression has no validation error.
     */
    public void hasNoValidationError(WbExpression<T> expression, ColumnModel columnModel) {
        ValidationState validationState = new ValidationState(columnModel);
        expression.validate(validationState);
        List<String> validationMessages = validationState.getValidationErrors()
                .stream()
                .map(e -> e.getMessage())
                .collect(Collectors.toList());
        if (!validationMessages.isEmpty()) {
            Assert.fail("Unexpected validation status: expected no error, but found errors: " + validationMessages.toString());
        }
    }

    /**
     * Sets the context to a row with the given values.
     * 
     * @param rowValues
     *            the list of row values. They can be cells or cell values.
     */
    public void setRow(Object... rowValues) {
        Row row = new Row(rowValues.length);
        for (int i = 0; i != rowValues.length; i++) {
            Object val = rowValues[i];
            if (Cell.class.isInstance(val)) {
                row.cells.add((Cell) val);
            } else {
                Cell cell = new Cell((Serializable) val, (Recon) null);
                row.cells.add(cell);
            }
        }
        ctxt = new ExpressionContext("http://www.wikidata.org/entity/", Collections.emptyMap(), server.url("/w/api.php").toString(), 0, row,
                project.columnModel, warningStore);
    }

    /**
     * Creates a make-shift reconciled cell for a given Qid.
     * 
     * @param qid
     * @return a cell for use in setRow
     */
    public Cell recon(String qid) {
        return TestingData.makeMatchedCell(qid, qid);
    }
}
