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

import java.io.IOException;
import java.io.Serializable;

import org.openrefine.wikidata.qa.QAWarningStore;
import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.testing.WikidataRefineTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

import com.google.refine.model.Cell;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Row;

public class WbExpressionTest<T> extends WikidataRefineTest {

    protected Project project;
    protected Row row;
    protected ExpressionContext ctxt;
    protected QAWarningStore warningStore;

    @BeforeMethod
    public void createProject()
            throws IOException, ModelException {
        project = createCSVProject("Wikidata variable test project",
                "column A,column B,column C,column D,column E\n" + "value A,value B,value C,value D,value E");
        warningStore = new QAWarningStore();
        row = project.rows.get(0);
        ctxt = new ExpressionContext("http://www.wikidata.org/entity/", 0, row, project.columnModel, warningStore);
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
        try {
            T result = expression.evaluate(ctxt);
            Assert.assertEquals(expected, result);
        } catch (SkipSchemaExpressionException e) {
            Assert.fail("Value was skipped by evaluator");
        }
    }

    /**
     * Test that a particular expression is skipped.
     * 
     * @param expected
     *            the expected evaluation of the value
     * @param expression
     *            the expression to evaluate
     */
    public void isSkipped(WbExpression<T> expression) {
        try {
            expression.evaluate(ctxt);
            Assert.fail("Value was not skipped by evaluator");
        } catch (SkipSchemaExpressionException e) {
            return;
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
        ctxt = new ExpressionContext("http://www.wikidata.org/entity/", 0, row, project.columnModel, warningStore);
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
