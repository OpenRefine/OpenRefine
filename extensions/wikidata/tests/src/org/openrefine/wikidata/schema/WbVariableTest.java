package org.openrefine.wikidata.schema;

import java.io.IOException;

import org.openrefine.wikidata.qa.QAWarningStore;
import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

import com.google.refine.model.Cell;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.tests.RefineTest;

public abstract class WbVariableTest<T> extends RefineTest {
    
    protected WbVariableExpr<T> variable;
    protected Project project;
    protected Row row;
    protected ExpressionContext ctxt;
    protected QAWarningStore warningStore;
    
    /**
     * This should return a variable expression, to be tested with the helpers below.
     * @return
     */
    public abstract WbVariableExpr<T> initVariableExpr();
    
    @BeforeMethod
    public void createProject() throws IOException, ModelException {
        project = createCSVProject("Wikidata variable test project", "column A\nrow1");
        warningStore = new QAWarningStore();
        row = project.rows.get(0);
        ctxt = new ExpressionContext("http://www.wikidata.org/entity/", 0,
                row, project.columnModel, warningStore);
        variable = initVariableExpr();
        variable.setColumnName("column A");
    }
    
    /**
     * Test that a particular cell value evaluates to some object
     * @param expected
     *          the expected evaluation of the value
     * @param input
     *          the cell value used by the variable
     */
    public void evaluatesTo(T expected, String input) {
        Cell cell = new Cell(input, null);
        evaluatesTo(expected, cell);
    }
    
    /**
     * Test that a particular cell evaluates to some object
     * @param expected
     *          the expected evaluation of the value
     * @param cell
     *          the cell used by the variable
     */
    public void evaluatesTo(T expected, Cell cell) {
        row.setCell(0, cell);
        try {
            T result = variable.evaluate(ctxt);
            Assert.assertEquals(expected, result);
        } catch (SkipSchemaExpressionException e) {
            Assert.fail("Value was skipped by evaluator");
        }
    }
    
    /**
     * Test that the variable rejects a particular cell value
     * @param input
     *          the cell value to reject
     */
    public void isSkipped(String input) {
        Cell cell = new Cell(input, null);
        isSkipped(cell);
    }

    /**
     * Test that a particular cell should be rejected by the variable
     * @param cell
     */
    protected void isSkipped(Cell cell) {
        row.setCell(0, cell);
        try {
            variable.evaluate(ctxt);
            Assert.fail("Value was not skipped by evaluator");
        } catch (SkipSchemaExpressionException e) {
            return;
        }
    }
}
