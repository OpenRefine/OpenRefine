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

import org.openrefine.wikibase.qa.QAWarning;
import org.testng.annotations.BeforeMethod;

import com.google.refine.model.Cell;
import com.google.refine.model.ModelException;

public abstract class WbVariableTest<T> extends WbExpressionTest<T> {

    protected WbVariableExpr<T> variable;

    /**
     * This should return a variable expression, to be tested with the helpers below.
     * 
     * @return
     */
    public abstract WbVariableExpr<T> initVariableExpr();

    @BeforeMethod
    public void setupVariable()
            throws IOException, ModelException {
        variable = initVariableExpr();
        variable.setColumnName("column A");
    }

    /**
     * Test that a particular cell value evaluates to some object
     * 
     * @param expected
     *            the expected evaluation of the value
     * @param input
     *            the cell value used by the variable
     */
    public void evaluatesTo(T expected, String input) {
        Cell cell = new Cell(input, null);
        evaluatesTo(expected, cell);
    }

    /**
     * Test that a particular cell evaluates to some object
     * 
     * @param expected
     *            the expected evaluation of the value
     * @param cell
     *            the cell used by the variable
     */
    public void evaluatesTo(T expected, Cell cell) {
        row.setCell(0, cell);
        evaluatesTo(expected, variable);
    }

    /**
     * Test that a particular cell evaluates to some warning
     * 
     * @param expected
     *            the expected evaluation of the value
     * @param cell
     *            the cell used by the variable
     */
    public void evaluatesToWarning(QAWarning expected, Cell cell) {
        row.setCell(0, cell);
        evaluatesToWarning(expected, variable);
    }

    /**
     * Test that the variable rejects a particular cell value
     * 
     * @param input
     *            the cell value to reject
     */
    public void isSkipped(String input) {
        Cell cell = new Cell(input, null);
        isSkipped(cell);
    }

    /**
     * Test that a particular cell should be rejected by the variable
     * 
     * @param cell
     */
    protected void isSkipped(Cell cell) {
        row.setCell(0, cell);
        isSkipped(variable);
    }
}
