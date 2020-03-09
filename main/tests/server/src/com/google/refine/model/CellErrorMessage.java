package com.google.refine.model;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.Cell;

import static org.testng.Assert.assertEquals;

import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;


public class CellErrorMessage extends RefineTest {

    protected Project project = null;
    protected String errorMessage = "Test Sample string";

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void setUpTestData(){
        project = createCSVProject(
                "first_column,second_column\n");
        Row row = new Row(1);
        row.setCell(0, new Cell(new Integer(1), null));
        row.setCell(1, new Cell(new EvalError(errorMessage), null));
        project.rows.add(row);
    }

    @Test
    public void testGetFielderror(){
        Cell check = project.rows.get(0).cells.get(1);
        // Check expression equivalent to 'cell.error'
        assertEquals(check.getErrorMessage(), errorMessage);
    }
}
